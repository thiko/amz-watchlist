package de.headstuff.amazonscraper.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.model.ProductScrapingResult;
import de.headstuff.amazonscraper.worker.ProductScrapingWorker;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@ApplicationScoped
public class ProductScraperService implements IProductScraperService<ProductScrapingResult> {

    @Inject
    ProductScrapingWorker productScrapingWorker;

    @Inject
    MongoClient mongoClient;

    @Scheduled(cron = "{cron.scraping}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void updateAll() {
        this.getAll().forEach(this::scrapeAndUpdateSingleEntry);
    }

    @ConsumeEvent("scrapeAndUpdate")
    @Blocking
    public void onScrapeAndUpdateEventReceived(ProductScrapingResult productScrapingResult) {
        if (scrapeAndUpdateSingleEntry(productScrapingResult).isPresent()) {
            log.debug("Event loop completed with result: {}", productScrapingResult.toString());
        }
    }

    public boolean isWorkerBusy() {
        return productScrapingWorker.isWorking();
    }

    public Optional<ProductScrapingResult> scrapeAndUpdateSingleEntry(ProductScrapingResult productScrapingResult) {
        try {
            log.debug("Start scraping data from {}", productScrapingResult.getProductUrl());
            val updatedResult = productScrapingWorker.scrapeProduct(productScrapingResult.getProductUrl());
            updatedResult.setUuid(productScrapingResult.getUuid());
            updatedResult.setProductUrl(productScrapingResult.getProductUrl());
            updatedResult.setLastSyncTimeUtc(new Date());
            updatedResult.setLastSyncSuccessful(true);
            log.debug("Scraping {} successfully.", productScrapingResult.getName());
            return this.insertOrReplace(updatedResult);
        } catch (Exception e) {
            productScrapingResult.setLastSyncTimeUtc(new Date());
            productScrapingResult.setLastSyncSuccessful(false);
            log.warn("Scraping {} was not successful ({}).", productScrapingResult.getName(), e.getMessage());
            return this.insertOrReplace(productScrapingResult);
        }
    }

    public long deleteScrapingResult(ProductScrapingResult productScrapingResult) {
        val deletionResult = getCollection().deleteOne(eq("uuid", productScrapingResult.getUuid()));
        return deletionResult.getDeletedCount();
    }

    @Override
    public ProductScrapingResult modelFromDocument(Document document) {
        return ProductScrapingResult.fromDocument(document);
    }

    @Override
    public MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("amazon-scraper").getCollection("scraping-results");
    }
}
