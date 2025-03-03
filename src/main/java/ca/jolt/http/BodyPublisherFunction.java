package ca.jolt.http;

import java.net.http.HttpRequest;

@FunctionalInterface
public interface BodyPublisherFunction {

    HttpRequest.Builder apply(HttpRequest.Builder builder, HttpRequest.BodyPublisher bodyPublisher);
}