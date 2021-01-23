package de.headstuff.amazonscraper.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemState {

    boolean productScraperWorking;
    boolean categoryScraperWorking;

}
