package de.headstuff.amazonscraper.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ProductScrapingResult extends AbstractScrapingResult {

    String name;
    String productUrl;
    String imageUrl;
    String bestSellerRank;
    String ranking;
    String votes;
    Date lastSyncTimeUtc;
    Boolean lastSyncSuccessful;

    public static ProductScrapingResult fromDocument(Document document) {
        return ProductScrapingResult.builder()
                .uuid(document.getString("uuid"))
                .name(document.getString("name"))
                .productUrl(document.getString("productUrl"))
                .imageUrl(document.getString("imageUrl"))
                .bestSellerRank(document.getString("bsr"))
                .ranking(document.getString("ranking"))
                .votes(document.getString("votes"))
                .lastSyncSuccessful(document.getBoolean("lastSyncSuccessful"))
                .lastSyncTimeUtc(document.getDate("lastSyncTime"))
                .build();
    }

    @Override
    public Document toDocument() {
        val document = new Document();

        if (uuid != null) {
            document.append("uuid", uuid);
        }
        document.append("name", ObjectUtils.defaultIfNull(name, ""))
                .append("productUrl", ObjectUtils.defaultIfNull(productUrl, ""))
                .append("imageUrl", ObjectUtils.defaultIfNull(imageUrl, ""))
                .append("bsr", ObjectUtils.defaultIfNull(bestSellerRank, ""))
                .append("ranking", ObjectUtils.defaultIfNull(ranking, ""))
                .append("votes", ObjectUtils.defaultIfNull(votes, ""))
                .append("lastSyncSuccessful", ObjectUtils.defaultIfNull(lastSyncSuccessful, ""))
                .append("lastSyncTime", ObjectUtils.defaultIfNull(lastSyncTimeUtc, ""));

        return document;
    }
}
