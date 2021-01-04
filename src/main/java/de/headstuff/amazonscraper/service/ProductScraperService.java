package de.headstuff.amazonscraper.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.model.ScrapingResult;
import de.headstuff.amazonscraper.worker.ScrapingWorker;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnItem;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

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

    public Optional<ScrapingResult> scrapeAndUpdateSingleEntry(ScrapingResult scrapingResult) {
        try {
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

    public UniOnItem<Optional<ScrapingResult>> scrapeAndUpdateSingleEntryReactive(ScrapingResult scrapingResult) {
        return Uni.createFrom().item(scrapeAndUpdateSingleEntry(scrapingResult))
                .onItem();
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
            val updatedEntryId = Objects.requireNonNull(getCollection().insertOne(document).getInsertedId());
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
