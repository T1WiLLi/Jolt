package io.github.t1willi.http.api;

import java.io.IOException;

public interface HttpClient extends AutoCloseable {
    HttpResponse async(String uri) throws IOException, InterruptedException;

    default HttpResponse sync(String uri) throws IOException, InterruptedException {
        return async(uri);
    }

    @Override
    void close();
}
