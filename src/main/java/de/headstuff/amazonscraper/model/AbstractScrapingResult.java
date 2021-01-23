package de.headstuff.amazonscraper.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.bson.Document;

@Data
@SuperBuilder
public abstract class AbstractScrapingResult {

    String uuid;

    public abstract Document toDocument();

}
