package de.headstuff.amazonscraper.worker;

import de.headstuff.amazonscraper.exception.ScrapingException;
import de.headstuff.amazonscraper.model.CategoryScrapingResult;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@Slf4j
@ApplicationScoped
public class ProductCategoryWorker extends AbstractScrapingWorker {

    @Inject
    ProductScrapingWorker productScrapingWorker;

    @SessionScoped
    private Set<String> cachedCategories = new HashSet<String>(100);


    public List<CategoryScrapingResult> scrapeProductCategories(String baseUrl) {

        val scrapedResults = new ArrayList<CategoryScrapingResult>(100);
        // map category name to its related DO representation

        try {
            var pageContent = getPageAsXml(baseUrl);

            if (pageContent.isBlank()) {
                throw new ScrapingException("Unable to parse page from URI: " + baseUrl);
            }

            // Jsoup parsing
            val doc = Jsoup.parse(pageContent, baseUrl);

            val initialCategory = CategoryScrapingResult.builder()
                    .categoryName("Initial category")
                    .categoryPath("./")
                    .build();

            appendAdditionalScrapingTargets(initialCategory, doc);
            scrapedResults.add(initialCategory);

            scrapedResults.addAll(scrapeRecursive(initialCategory.getAdditionalScrapingTargets(),
                    initialCategory.getCategoryPath()));

            return scrapedResults;

        } catch (IOException e) {
            throw new ScrapingException(e);
        } finally {
            cachedCategories.clear();
        }
    }

    private List<CategoryScrapingResult> scrapeRecursive(Map<String, String> productLinkMap, String startingPointPath) {
        val scrapedResults = new ArrayList<CategoryScrapingResult>(100);

        for (val nextCategory : productLinkMap.entrySet()) {

            if (cachedCategories.contains(nextCategory.getKey())) {
                continue;
            }

            val cat = CategoryScrapingResult.builder()
                    .categoryName(nextCategory.getKey())
                    .categoryUrl(nextCategory.getValue())
                    .categoryPath(startingPointPath + " -> " + nextCategory.getKey())
                    .build();

            try {
                appendProductMetaDataFromOverviewPage(cat);

                // TODO: We could do this later as well
                cat.setHighestRankedProduct(productScrapingWorker.scrapeProduct(cat.getHighestRankedProductLink()));

                Thread.sleep(new Random().nextInt(9000) + 1000);
                cat.setLastSyncSuccessful(false);
                cat.setLastSyncTimeUtc(new Date());

                scrapedResults.add(cat);
                cachedCategories.add(cat.getCategoryName());

            } catch (Exception ex) {
                log.warn("Exceptional skip of: {} ({})", cat.getCategoryUrl(), ex.getMessage());
                cat.setLastSyncSuccessful(false);
                cat.setLastSyncTimeUtc(new Date());
            }

            val next= cat.getAdditionalScrapingTargets();
            val path = cat.getCategoryPath() != null ? cat.getCategoryPath() : "";
            if(next != null) {
                scrapedResults.addAll(scrapeRecursive(next, path));
            }
        }

        return scrapedResults;
    }

    private void appendProductMetaDataFromOverviewPage(CategoryScrapingResult currentScrapingResult) throws IOException {
        var pageContent = getPageAsXml(currentScrapingResult.getCategoryUrl());
        val doc = Jsoup.parse(pageContent, currentScrapingResult.getCategoryUrl());
        val elementContainer = doc.getElementById("zg-ordered-list");

        if (elementContainer == null) {
            log.warn("Unable to scrape product meta data from {}", currentScrapingResult.getCategoryUrl());
            return;
        }

        val allItems = elementContainer.select(".zg-item-immersion");
        // select the first one
        val bestProduct = allItems.stream()
                .filter(element -> element.text().contains("#1")).findFirst();

        if (bestProduct.isEmpty()) {
            return;
        }

        val productLink = bestProduct.get().select("a").attr("href");
        if (productLink == null || productLink.length() == 0) {
            return;
        }
        currentScrapingResult.setHighestRankedProductLink("https://amazon.de/" + productLink);
        this.appendAdditionalScrapingTargets(currentScrapingResult, doc);
    }


    private void appendAdditionalScrapingTargets(CategoryScrapingResult currentScrapingResult, Document doc) {
        val ulContainer = doc.getElementById("zg_browseRoot");
        if (ulContainer == null) {
            return;
        }
        val targetUl = ulContainer.select("a");
        if (targetUl == null) {
            return;
        }
        val targetMap = new HashMap<String, String>();

        for (val element : targetUl) {
            if (element.childNodes().size() > 0 &&
                    element.textNodes().size() > 0
                    && !element.textNodes().get(0).text().trim().equals("Alle Kategorien")) {
                targetMap.put(
                        element.textNodes().get(0).text().trim(),
                        element.attr("href"));
            }
        }

        currentScrapingResult.setAdditionalScrapingTargets(targetMap);
    }
}