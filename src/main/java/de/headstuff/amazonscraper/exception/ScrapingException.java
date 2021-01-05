package de.headstuff.amazonscraper.exception;

public class ScrapingException extends RuntimeException {

  public ScrapingException(String message) {
    super(message);
  }

  public ScrapingException(Throwable cause) {
    super(cause);
  }
}

