package de.headstuff.amazonscraper.worker;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import de.headstuff.amazonscraper.exception.ScrapingException;
import de.headstuff.amazonscraper.model.ScrapingResult;

import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;

import lombok.val;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

@ApplicationScoped
public class ScrapingWorker {

    private final String BSR_REGEX = ".*Nr\\.\\W(\\d+[,.]?\\d*)\\Win Bücher.*";

    public ScrapingResult scrapeProduct(String targetUrl) {

        if (targetUrl.isEmpty()) {
            throw new ScrapingException("Unable to scrape the page without an URL");
        }

        try {
            var pageAsXml = "";
            // load page using HTML Unit and fire scripts
            try (val webClient = new WebClient(BrowserVersion.CHROME)) {
                webClient.getOptions().setUseInsecureSSL(true);
                webClient.getOptions().setRedirectEnabled(true);
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                webClient.getCookieManager().setCookiesEnabled(false);
                webClient.getOptions().setTimeout(10 * 1000); // Set the connection timeout
                webClient.getOptions().setDownloadImages(false);
                webClient.getOptions().setGeolocationEnabled(false);
                webClient.getOptions().setAppletEnabled(false);

                HtmlPage page = webClient.getPage(targetUrl);
                webClient.waitForBackgroundJavaScript(
                        30 * 1000); // Wait for js to execute in the background for 30 seconds
                webClient.setJavaScriptTimeout(35 * 1000);

                pageAsXml = page.asXml();
            }

            if (pageAsXml.isBlank()) {
                throw new ScrapingException("Unable to parse page from URI: " + targetUrl);
            }

            // Jsoup parsing
            val doc = Jsoup.parse(pageAsXml, targetUrl);

            return ScrapingResult.builder()
                    .bestSellerRank(extractBsr(doc).orElse(""))
                    .name(extractTitle(doc).orElse(""))
                    .imageUrl(extractImageUrl(doc).orElse(""))
                    .ranking(extractRanking(doc).orElse(""))
                    .votes(extractVotes(doc).orElse(""))
                    .build();
        } catch (IOException | ParseException ex) {
            throw new ScrapingException(ex);
        }
    }

    private Optional<String> extractVotes(Document doc) {
        val votes = doc.getElementById("acrCustomerReviewText");
        if (votes != null && votes.text() != null) {
            return Optional.of(votes.text().split(" ")[0]);
        }
        return Optional.empty();
    }

    private Optional<String> extractRanking(Document doc) {
        val parent = doc.getElementById("acrPopover");
        val ratingSpan = parent.children().stream().filter(element -> element.text().contains("von 5")).findFirst();

        if (ratingSpan.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ratingSpan.get().text().split(" ")[0]);
    }

    private Optional<String> extractTitle(Document doc) {
        val titleElement = doc.getElementById("productTitle");
        if (titleElement != null && titleElement.text() != null) {
            return Optional.of(titleElement.text());
        }
        return Optional.empty();
    }

    private Optional<String> extractImageUrl(Document doc) {
        val imageElement = doc.getElementById("imgBlkFront");
        if (imageElement != null && imageElement.attr("src") != null) {
            return Optional.of(imageElement.attr("src"));
        }
        return Optional.empty();
    }


    private Optional<String> extractBsr(Document doc) throws ParseException {
        val allSpans = doc.select(".a-text-bold");
        val targetSpan = allSpans.stream()
                .filter(element -> element.text().contains("Amazon Bestseller-Rang")).findFirst();

        if (targetSpan.isPresent()) {
            val bsr = targetSpan.get().parent().childNodes().stream().filter(
                    child -> child instanceof TextNode && ((TextNode) child).text().matches(BSR_REGEX))
                    .findFirst();
            if (bsr.isEmpty()) {
                return Optional.empty();
            }
            val bsrRawValue = ((TextNode) bsr.get()).text();
            return extractBsrValue(bsrRawValue);
        }
        return Optional.empty();
    }

    private Optional<String> extractBsrValue(String rawValue) throws ParseException {
        Pattern p = Pattern.compile(BSR_REGEX);
        Matcher m = p.matcher(rawValue);
        if (m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }
}
