package io.github.t1willi.security.authentification;

import io.github.t1willi.context.JoltContext;

public class NoOpAuthFailureHandler implements OnAuthFailure {

    @Override
    public void handle(JoltContext context) {
        // No-Op
    }
}
