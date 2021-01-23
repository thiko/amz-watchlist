package de.headstuff.amazonscraper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.Document;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractScrapingResult {

    String uuid;

    public abstract Document toDocument();

}
