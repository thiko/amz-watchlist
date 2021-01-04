package de.headstuff.amazonscraper;

import de.headstuff.amazonscraper.exception.ScrapingException;
import de.headstuff.amazonscraper.model.ScrapingResult;
import de.headstuff.amazonscraper.service.ProductScraperService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@Path("/products")
@Slf4j
public class ProductResource {

    @Inject
    ProductScraperService productScraperService;

    @Inject
    ManagedExecutor exec;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ScrapingResult> getAllProducts() throws ScrapingException {
        return productScraperService.getAll();
    }

    @POST
    public void insertOrReplace(ScrapingResult scrapingResult) {
        val inserted = productScraperService.insertOrReplace(scrapingResult);
        log.info("New product added");
        if (inserted.isPresent()) {
            exec.runAsync(() -> {productScraperService.scrapeAndUpdateSingleEntry(inserted.get());});
        } else {
            log.warn("Unable to scrape data - insert or update fails for URL: {}", scrapingResult.getProductUrl());
        }
    }

    @DELETE
    public void delete(ScrapingResult scrapingResult) {
        val deletedEntries = productScraperService.deleteScrapingResult(scrapingResult);
        log.info("{} entries deleted", deletedEntries);
    }
}