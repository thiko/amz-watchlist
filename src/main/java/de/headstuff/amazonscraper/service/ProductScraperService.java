package de.headstuff.amazonscraper.service;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.model.ProductScrapingResult;
import de.headstuff.amazonscraper.worker.ProductScrapingWorker;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.Document;
import org.bson.types.ObjectId;

@Slf4j
@ApplicationScoped
public class ProductScraperService {

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
      log.warn("Scraping {} was not successful.", productScrapingResult.getName());
      return this.insertOrReplace(productScrapingResult);
    }
  }

  public List<ProductScrapingResult> getAll() {
    val scrapingResults = new ArrayList<ProductScrapingResult>();

    try (val cursor = getCollection().find().iterator()) {
      while (cursor.hasNext()) {
        val document = cursor.next();
        scrapingResults.add(ProductScrapingResult.fromDocument(document));
      }
    }

    return scrapingResults;
  }

  public Optional<ProductScrapingResult> getScrapingResultById(ObjectId id) {
    val doc = getCollection().find().filter(eq("_id", id)).first();
    if (doc == null) {
      return Optional.empty();
    }
    return Optional.of(ProductScrapingResult.fromDocument(doc));
  }

  public Optional<ProductScrapingResult> getScrapingResultByUuid(String uuid) {
    val doc = getCollection().find().filter(eq("uuid", uuid)).first();
    if (doc == null) {
      return Optional.empty();
    }
    return Optional.of(ProductScrapingResult.fromDocument(doc));
  }

  public long deleteScrapingResult(ProductScrapingResult productScrapingResult) {
    val deletionResult = getCollection().deleteOne(eq("uuid", productScrapingResult.getUuid()));
    return deletionResult.getDeletedCount();
  }

  public Optional<ProductScrapingResult> insertOrReplace(ProductScrapingResult productScrapingResult) {
    log.debug("Insert or replace data: {}", productScrapingResult.toString());
    String uuid = "";
    boolean insertNewEntry = false;
    if (productScrapingResult.getUuid() != null && !productScrapingResult.getUuid().isEmpty()) {
      uuid = productScrapingResult.getUuid();
    } else {
      uuid = UUID.randomUUID().toString();
      insertNewEntry = true;
    }

    val document = new Document()
        .append("uuid", uuid)
        .append("name", productScrapingResult.getName())
        .append("productUrl", productScrapingResult.getProductUrl())
        .append("imageUrl", productScrapingResult.getImageUrl())
        .append("bsr", productScrapingResult.getBestSellerRank())
        .append("ranking", productScrapingResult.getRanking())
        .append("votes", productScrapingResult.getVotes())
        .append("lastSyncSuccessful", productScrapingResult.getLastSyncSuccessful())
        .append("lastSyncTime", productScrapingResult.getLastSyncTimeUtc());

    if (insertNewEntry) {
      val updatedEntryId = Objects
          .requireNonNull(getCollection().insertOne(document).getInsertedId());
      return getScrapingResultById(updatedEntryId.asObjectId().getValue());
    } else {
      getCollection().replaceOne(eq("uuid", uuid), document);
      return getScrapingResultByUuid(uuid);
    }
  }

  private MongoCollection<Document> getCollection() {
    return mongoClient.getDatabase("amazon-scraper").getCollection("scraping-results");
  }
}
