package de.headstuff.amazonscraper;

import de.headstuff.amazonscraper.model.ScrapingResult;
import de.headstuff.amazonscraper.service.ProductScraperService;
import io.vertx.mutiny.core.eventbus.EventBus;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Produces("application/json")
@Path("/products")
@Slf4j
public class ProductResource {

  @Inject
  ProductScraperService productScraperService;

  @Inject
  EventBus bus;


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ScrapingResult> getAllProducts() {
    return productScraperService.getAll();
  }

  @POST
  public void insertOrReplace(ScrapingResult scrapingResult) {
    val inserted = productScraperService.insertOrReplace(scrapingResult);
    if (inserted.isPresent()) {
      log.info("Product added/replaced: {}", inserted.get().toString());
      bus.sendAndForget("scrapeAndUpdate", inserted.get());
    } else {
      log.warn("Unable to scrape data - insert or update fails for URL: {}",
          scrapingResult.getProductUrl());
    }
  }

  @DELETE
  public void delete(ScrapingResult scrapingResult) {
    val deletedEntries = productScraperService.deleteScrapingResult(scrapingResult);
    log.info("{} entries deleted", deletedEntries);
  }
}