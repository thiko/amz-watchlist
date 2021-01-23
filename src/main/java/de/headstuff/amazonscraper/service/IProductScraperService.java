package de.headstuff.amazonscraper.service;

import com.mongodb.client.MongoCollection;
import de.headstuff.amazonscraper.model.AbstractScrapingResult;
import lombok.val;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public interface IProductScraperService<T extends AbstractScrapingResult> {

    default List<T> getAll() {
        val scrapingResults = new ArrayList<T>();

        try (val cursor = getCollection().find().iterator()) {
            while (cursor.hasNext()) {
                val document = cursor.next();
                scrapingResults.add(modelFromDocument(document));
            }
        }

        return scrapingResults;
    }


    default Optional<T> getScrapingResultByUuid(String uuid) {
        val doc = getCollection().find().filter(eq("uuid", uuid)).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(modelFromDocument(doc));
    }

    default Optional<T> getScrapingResultById(ObjectId id) {
        val doc = getCollection().find().filter(eq("_id", id)).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(modelFromDocument(doc));
    }

    default Optional<T> insertOrReplace(T scrapingResult) {
        String uuid = "";
        boolean insertNewEntry = false;
        if (scrapingResult.getUuid() != null && !scrapingResult.getUuid().isEmpty()) {
            uuid = scrapingResult.getUuid();
        } else {
            uuid = UUID.randomUUID().toString();
            insertNewEntry = true;
        }

        val document = scrapingResult.toDocument();
        document.append("uuid", uuid);

        if (insertNewEntry) {
            val updatedEntryId = Objects
                    .requireNonNull(getCollection().insertOne(document).getInsertedId());
            return getScrapingResultById(updatedEntryId.asObjectId().getValue());
        } else {
            getCollection().replaceOne(eq("uuid", uuid), document);
            return getScrapingResultByUuid(uuid);
        }
    }

    T modelFromDocument(Document document);

    MongoCollection<Document> getCollection();
}
