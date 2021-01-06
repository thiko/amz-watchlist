package de.headstuff.amazonscraper.model;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import org.bson.Document;

@Data
@Builder
public class ScrapingResult {

  String uuid;
  String name;
  String productUrl;
  String imageUrl;
  String bestSellerRank;
  String ranking;
  String votes;
  Date lastSyncTimeUtc;
  Boolean lastSyncSuccessful;

  public static ScrapingResult fromDocument(Document document) {
    return ScrapingResult.builder()
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
}
