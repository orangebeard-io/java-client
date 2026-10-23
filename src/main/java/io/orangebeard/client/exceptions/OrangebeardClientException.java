package io.orangebeard.client.exceptions;

/**
 * Thrown by the synchronous client when a request could not be completed (after retries).
 */
public class OrangebeardClientException extends RuntimeException {
    public OrangebeardClientException(String message) {
        super(message);
    }

    public OrangebeardClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
