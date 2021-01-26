package de.headstuff.amazonscraper.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.exception.ScrapingException;
import de.headstuff.amazonscraper.model.CategoryScrapingResult;
import de.headstuff.amazonscraper.worker.ProductCategoryWorker;
import de.headstuff.amazonscraper.worker.ProductScrapingWorker;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@ApplicationScoped
public class ProductCategoryScraperService implements IProductScraperService<CategoryScrapingResult> {

    private static final String baseUrl = "https://www.amazon.de/gp/bestsellers/books/ref=zg_bs_nav_0";

    @Inject
    ProductCategoryWorker productCategoryWorker;

    @Inject
    ProductScrapingWorker productScrapingWorker;

    @Inject
    MongoClient mongoClient;

    @ConsumeEvent("scrapeCategories")
    @Blocking
    public void onEventReceived(CategoryScrapingMode mode) {
        if (CategoryScrapingMode.COMPLETE == mode) {
            this.scrapeAndStoreAllCategories();
        } else if (CategoryScrapingMode.ONLY_PRODUCTS == mode) {
            this.updateHighestRankedProductsInCategories();
        } else if (CategoryScrapingMode.PRODUCT_LINKS_AND_PRODUCTS == mode) {
            throw new NotImplementedException("PRODUCT_LINKS_AND_PRODUCTS isn't implemented yet.");
        }
    }

    public void scrapeAndStoreAllCategories() {
        log.info("Start scraping all categories");
        val allCategories = productCategoryWorker.scrapeProductCategories(baseUrl);
        // delete all documents and indexes
        this.getCollection().drop();
        log.info("All category-related documents dropped");
        // store new categories
        allCategories.forEach(this::insertOrReplace);
        log.info("{} categories added to mongodb", allCategories.size());
    }

    public boolean isWorkerBusy() {
        return productCategoryWorker.isWorking();
    }

    /**
     * Iterates through all categories and re-scrape the highest ranked product links
     *
     * @param categoryScrapingResults - all categories which needs to be updated
     */
    private void updateHighestRankedProductLinks(List<CategoryScrapingResult> categoryScrapingResults) {
        // TODO: implement
    }

    /**
     * Iterates through all categories and updates the linked products. The product-link itself does not get updated.
     */
    private void updateHighestRankedProductsInCategories() {
        log.info("Start scraping highest ranked products for existing categories");
        updateHighestRankedProductsInCategories(getAll());
    }

    /**
     * Iterates through all given categories and updates the linked products. The product-link itself does not get updated.
     *
     * @param categoryScrapingResults - the categories
     */
    private void updateHighestRankedProductsInCategories(List<CategoryScrapingResult> categoryScrapingResults) {
        val validCategories = categoryScrapingResults.stream()
                .filter(cat -> cat.getHighestRankedProductLink() != null && !cat.getHighestRankedProductLink().isEmpty())
                .collect(Collectors.toList());

        for (val cat : validCategories) {
            try {
                cat.setLastSyncTimeUtc(new Date());
                cat.setLastSyncSuccessful(true);

                // update the embedded product
                cat.setHighestRankedProduct(productScrapingWorker.scrapeProduct(cat.getHighestRankedProductLink()));
            } catch (ScrapingException ex) {
                log.warn("Unable to update highest ranked product of {} ({})", cat.getCategoryName(), cat.getCategoryUrl());
                cat.setLastSyncSuccessful(false);
            }

            // store the update
            this.insertOrReplace(cat);
        }
    }

    @Override
    public CategoryScrapingResult modelFromDocument(Document document) {
        return CategoryScrapingResult.fromDocument(document);
    }

    @Override
    public MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("amazon-scraper").getCollection("category-scraping-results");
    }
}
