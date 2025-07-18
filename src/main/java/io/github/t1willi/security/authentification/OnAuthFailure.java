package io.github.t1willi.security.authentification;

import io.github.t1willi.context.JoltContext;

@FunctionalInterface
public interface OnAuthFailure {

    /**
     * Called when authentification fails.
     * <p>
     * The default implementation won't do anything. You can override this method to
     * handle the failure in your own way.
     * As this returns a ResponseEntity<?>, you can return pretty much anything you
     * want.
     * 
     * @param context The JoltContext associated with the request.
     */
    void handle(JoltContext context);
}
