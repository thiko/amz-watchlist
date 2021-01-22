package de.headstuff.amazonscraper.worker;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.val;

import java.io.IOException;

public class AbstractScrapingWorker {

    protected String getPageAsXml(String targetUrl) throws IOException {
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

            return page.asXml();
        }
    }
}
