package de.headstuff.amazonscraper;


import de.headstuff.amazonscraper.model.CategoryScrapingResult;
import de.headstuff.amazonscraper.service.ProductCategoryScraperService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces("application/json")
@Path("/categories")
@Slf4j
public class ProductCategoryResource {

    @Inject
    ProductCategoryScraperService productCategoryScraperService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CategoryScrapingResult> getAllProducts() {
        return productCategoryScraperService.scrapeAllRecursive();
    }
}
