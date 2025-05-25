package io.github.t1willi.core;

import io.github.t1willi.http.ModelView;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.template.JoltModel;

/**
 * An abstract base class for MVC controllers, providing utility methods to
 * render views and handle redirects.
 * <p>
 * This class extends {@link BaseController} and offers methods to create HTTP
 * responses for rendering
 * HTML views or redirecting to other locations in a Model-View-Controller (MVC)
 * architecture. It is
 * designed for use in web applications where controllers need to render
 * templates with data models
 * or redirect clients to different URLs. The methods support the construction
 * of {@link ResponseEntity}
 * objects with appropriate headers and content, such as HTML content types for
 * rendered views or
 * redirect responses with location headers. Subclasses can use these methods to
 * implement MVC
 * controller logic in a consistent and standardized way.
 *
 * @since 1.0.0
 */
public abstract class MvcController extends BaseController {

    /**
     * Renders an HTML view with the specified model data.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 200 (OK)
     * and a {@link ModelView}
     * that encapsulates the specified view name and data model. If the provided
     * model is null, an empty
     * {@link JoltModel} is used to ensure safe rendering. The response includes a
     * "Content-Type" header
     * set to "text/html". This method is typically used in MVC controllers to
     * render HTML templates
     * populated with data from the model, such as in response to a GET request in a
     * web application.
     *
     * @param view  the name of the view template to render (e.g., the name of an
     *              HTML template file)
     * @param model the {@link JoltModel} containing data to populate the view, or
     *              null for an empty model
     * @return a {@link ResponseEntity} with HTTP status 200, HTML content type, and
     *         the rendered view
     * @throws IllegalArgumentException    if the view name is null or empty
     * @throws TemplateProcessingException if the view cannot be rendered due to
     *                                     template errors
     * @since 1.0.0
     */
    protected ResponseEntity<ModelView> render(String view, JoltModel model) {
        JoltModel m = model != null ? model : JoltModel.empty();
        return ResponseEntity.ok(ModelView.of(view, m))
                .header("Content-Type", "text/html");
    }

    /**
     * Constructs an HTTP 302 Found response to redirect the client to the specified
     * location.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 302
     * (Found) and a "Location"
     * header set to the provided URI. It is used in MVC controllers to redirect
     * clients to another URL,
     * typically after processing a POST request or when navigating to a different
     * page. The response
     * has no body content, as the redirect instructs the client to issue a new
     * request to the specified
     * location.
     *
     * @param location the URI to which the client should be redirected
     * @return a {@link ResponseEntity} with HTTP status 302 and the specified
     *         location header
     * @throws IllegalArgumentException if the location is null or an invalid URI
     * @since 1.0.0
     */
    protected ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.redirect(location);
    }

    /**
     * Constructs an HTTP 302 Found response to redirect the client to the specified
     * location with a model.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 302
     * (Found), a "Location" header
     * set to the provided URI, and a {@link JoltModel} included in the response.
     * The model may be used
     * by the framework to pass data to the redirected endpoint, such as for flash
     * attributes in a
     * POST-redirect-GET pattern. The response includes a "Content-Type" header set
     * to "text/html". This
     * method is useful in MVC controllers when a redirect needs to carry model data
     * to the target
     * endpoint.
     *
     * @param location the URI to which the client should be redirected
     * @param model    the {@link JoltModel} containing data to be passed to the
     *                 redirected endpoint
     * @return a {@link ResponseEntity} with HTTP status 302, HTML content type, and
     *         the specified model
     * @throws IllegalArgumentException if the location is null or an invalid URI
     * @since 1.0.0
     */
    protected ResponseEntity<JoltModel> redirect(String location, JoltModel model) {
        return ResponseEntity.redirect(location, model)
                .header("Content-Type", "text/html");
    }
}