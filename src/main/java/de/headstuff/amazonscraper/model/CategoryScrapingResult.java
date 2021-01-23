package de.headstuff.amazonscraper.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class CategoryScrapingResult extends AbstractScrapingResult {

    String categoryName;
    String categoryPath;
    String categoryUrl;
    String highestRankedProductLink;
    ProductScrapingResult highestRankedProduct;
    Date lastSyncTimeUtc;
    Boolean lastSyncSuccessful;

    // Further category links (name, link)
    Map<String, String> additionalScrapingTargets;


    @Override
    public Document toDocument() {
        val doc = new Document();
        if (uuid != null && !uuid.isEmpty())
            doc.append("uuid", uuid);

        doc.append("categoryName", ObjectUtils.defaultIfNull(categoryName, ""));
        doc.append("categoryPath", ObjectUtils.defaultIfNull(categoryPath, ""));
        doc.append("categoryUrl", ObjectUtils.defaultIfNull(categoryUrl, ""));
        doc.append("highestRankedProductLink", ObjectUtils.defaultIfNull(highestRankedProductLink, ""));
        doc.append("lastSyncTimeUtc", ObjectUtils.defaultIfNull(lastSyncTimeUtc, new Date()));
        doc.append("lastSyncSuccessful", ObjectUtils.defaultIfNull(lastSyncSuccessful, ""));

        if (highestRankedProduct != null)
            doc.append("highestRankedProduct", highestRankedProduct);

        return doc;
    }


    public static CategoryScrapingResult fromDocument(Document document) {
        return CategoryScrapingResult.builder()
                .uuid(document.getString("uuid"))
                .categoryName(document.getString("categoryName"))
                .categoryPath(document.getString("categoryPath"))
                .categoryUrl(document.getString("categoryUrl"))
                .highestRankedProductLink(document.getString("highestRankedProductLink"))
                .highestRankedProduct(document.getEmbedded(Collections.singletonList("ranking"), ProductScrapingResult.class))
                .lastSyncSuccessful(document.getBoolean("lastSyncSuccessful"))
                .lastSyncTimeUtc(document.getDate("lastSyncTimeUtc"))
                .build();
    }

}

