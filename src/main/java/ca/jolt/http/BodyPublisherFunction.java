package ca.jolt.http;

import java.net.http.HttpRequest;

/**
 * Functional interface that represents a function for modifying an
 * {@link HttpRequest.Builder} with a given {@link HttpRequest.BodyPublisher}.
 * 
 * <p>
 * Implementations of this interface should define how the provided
 * {@code bodyPublisher} is applied to the {@code builder}.
 * 
 * <p>
 * <strong>Example usage:</strong>
 * 
 * <pre>{@code
 * BodyPublisherFunction publisherFunction = (builder, body) -> builder.POST(body);
 * HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create("https://example.com"));
 * HttpRequest request = publisherFunction.apply(requestBuilder, HttpRequest.BodyPublishers.ofString("data")).build();
 * }</pre>
 * 
 * @author William Beaudin
 * @since 1.0
 */
@FunctionalInterface
public interface BodyPublisherFunction {

    /**
     * Applies the given {@link HttpRequest.BodyPublisher} to the provided
     * {@link HttpRequest.Builder}.
     * 
     * @param builder       the {@code HttpRequest.Builder} to modify
     * @param bodyPublisher the {@code BodyPublisher} containing the request body
     * @return the modified {@code HttpRequest.Builder}
     */
    HttpRequest.Builder apply(HttpRequest.Builder builder, HttpRequest.BodyPublisher bodyPublisher);
}
