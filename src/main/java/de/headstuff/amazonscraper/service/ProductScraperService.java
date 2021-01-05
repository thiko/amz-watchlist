package de.headstuff.amazonscraper.service;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.model.ScrapingResult;
import de.headstuff.amazonscraper.worker.ScrapingWorker;
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
  ScrapingWorker scrapingWorker;

  @Inject
  MongoClient mongoClient;

  @Scheduled(cron = "{cron.scraping}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  public void updateAll() {
    this.getAll().forEach(this::scrapeAndUpdateSingleEntry);
  }

  @ConsumeEvent("scrapeAndUpdate")
  @Blocking
  public void onScrapeAndUpdateEventReceived(ScrapingResult scrapingResult) {
    if (scrapeAndUpdateSingleEntry(scrapingResult).isPresent()) {
      log.debug("Event loop completed with result: {}", scrapingResult.toString());
    }
  }

  public Optional<ScrapingResult> scrapeAndUpdateSingleEntry(ScrapingResult scrapingResult) {
    try {
      log.debug("Start scraping data from {}", scrapingResult.getProductUrl());
      val updatedResult = scrapingWorker.scrapeProduct(scrapingResult.getProductUrl());
      updatedResult.setUuid(scrapingResult.getUuid());
      updatedResult.setProductUrl(scrapingResult.getProductUrl());
      updatedResult.setLastSyncTimeUtc(new Date());
      updatedResult.setLastSyncSuccessful(true);
      log.debug("Scraping {} successfully.", scrapingResult.getName());
      return this.insertOrReplace(updatedResult);
    } catch (Exception e) {
      scrapingResult.setLastSyncTimeUtc(new Date());
      scrapingResult.setLastSyncSuccessful(false);
      log.warn("Scraping {} was not successful.", scrapingResult.getName());
      return this.insertOrReplace(scrapingResult);
    }
  }

  public List<ScrapingResult> getAll() {
    val scrapingResults = new ArrayList<ScrapingResult>();

    try (val cursor = getCollection().find().iterator()) {
      while (cursor.hasNext()) {
        val document = cursor.next();
        scrapingResults.add(ScrapingResult.fromDocument(document));
      }
    }

    return scrapingResults;
  }

  public Optional<ScrapingResult> getScrapingResultById(ObjectId id) {
    val doc = getCollection().find().filter(eq("_id", id)).first();
    if (doc == null) {
      return Optional.empty();
    }
    return Optional.of(ScrapingResult.fromDocument(doc));
  }

  public Optional<ScrapingResult> getScrapingResultByUuid(String uuid) {
    val doc = getCollection().find().filter(eq("uuid", uuid)).first();
    if (doc == null) {
      return Optional.empty();
    }
    return Optional.of(ScrapingResult.fromDocument(doc));
  }

  public long deleteScrapingResult(ScrapingResult scrapingResult) {
    val deletionResult = getCollection().deleteOne(eq("uuid", scrapingResult.getUuid()));
    return deletionResult.getDeletedCount();
  }

  public Optional<ScrapingResult> insertOrReplace(ScrapingResult scrapingResult) {
    log.debug("Insert or replace data: {}", scrapingResult.toString());
    String uuid = "";
    boolean insertNewEntry = false;
    if (scrapingResult.getUuid() != null && !scrapingResult.getUuid().isEmpty()) {
      uuid = scrapingResult.getUuid();
    } else {
      uuid = UUID.randomUUID().toString();
      insertNewEntry = true;
    }

    val document = new Document()
        .append("uuid", uuid)
        .append("name", scrapingResult.getName())
        .append("productUrl", scrapingResult.getProductUrl())
        .append("imageUrl", scrapingResult.getImageUrl())
        .append("bsr", scrapingResult.getBestSellerRank())
        .append("lastSyncSuccessful", scrapingResult.getLastSyncSuccessful())
        .append("lastSyncTime", scrapingResult.getLastSyncTimeUtc());

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
