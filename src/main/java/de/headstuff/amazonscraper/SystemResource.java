package de.headstuff.amazonscraper;

import de.headstuff.amazonscraper.model.SystemState;
import de.headstuff.amazonscraper.service.ProductCategoryScraperService;
import de.headstuff.amazonscraper.service.ProductScraperService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Produces("application/json")
@Path("/system")
@Slf4j
public class SystemResource {

    @Inject
    ProductScraperService productScraperService;

    @Inject
    ProductCategoryScraperService productCategoryScraperService;

    @GET
    public SystemState getSystemState() {

        return SystemState.builder()
                .productScraperWorking(productScraperService.isWorkerBusy())
                .categoryScraperWorking(productCategoryScraperService.isWorkerBusy())
                .build();
    }
}
