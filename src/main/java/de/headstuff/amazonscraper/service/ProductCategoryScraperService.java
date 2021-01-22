package de.headstuff.amazonscraper.service;

import de.headstuff.amazonscraper.model.CategoryScrapingResult;
import de.headstuff.amazonscraper.worker.ProductCategoryWorker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;


@Slf4j
@ApplicationScoped
public class ProductCategoryScraperService {

    private static final String baseUrl = "https://www.amazon.de/gp/bestsellers/books/ref=zg_bs_nav_0";

    @Inject
    ProductCategoryWorker productCategoryWorker;

    public List<CategoryScrapingResult> scrapeAllRecursive() {

        return productCategoryWorker.scrapeProductCategories(baseUrl);
    }
}
