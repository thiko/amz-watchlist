package de.headstuff.amazonscraper;


import de.headstuff.amazonscraper.model.CategoryScrapingResult;
import de.headstuff.amazonscraper.service.CategoryScrapingMode;
import de.headstuff.amazonscraper.service.ProductCategoryScraperService;
import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces("application/json")
@Path("/categories")
@Slf4j
public class ProductCategoryResource {

    @Inject
    ProductCategoryScraperService productCategoryScraperService;

    @Inject
    EventBus bus;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CategoryScrapingResult> getAllProducts() {
        return productCategoryScraperService.getAll();
    }

    @POST
    public void triggerUpdateCategories(@QueryParam("mode")CategoryScrapingMode mode) {
        if(productCategoryScraperService.isWorkerBusy()) {
            log.warn("Worker cannot handle additional workload - request ignored");
            return;
        }
        bus.sendAndForget("scrapeCategories", mode);
    }
}
