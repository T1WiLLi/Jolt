package io.github.t1willi.security.csrf;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.config.CsrfConfiguration;

public interface CsrfHandler {
    void validate(JoltContext context, CsrfConfiguration config) throws JoltHttpException;

    void sendToken(JoltContext context, String token, CsrfConfiguration config);

    String receiveToken(JoltContext context, CsrfConfiguration config);
}