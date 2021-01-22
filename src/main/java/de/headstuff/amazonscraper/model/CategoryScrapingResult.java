package de.headstuff.amazonscraper.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Builder
@Data
public class CategoryScrapingResult {

    String categoryName;
    String categoryPath;
    String categoryUrl;
    // make scraping easier
    String highestRankedProductLink;
    ProductScrapingResult highestRankedProduct;
    Date lastSyncTimeUtc;
    Boolean lastSyncSuccessful;

    // Further category links (name, link)
    Map<String, String> additionalScrapingTargets;

    public boolean isComplete() {
        return !Objects.isNull(categoryName)
                && !categoryName.isBlank()
                && !Objects.isNull(highestRankedProduct);
    }

}

