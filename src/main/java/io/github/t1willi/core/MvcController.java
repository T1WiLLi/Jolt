package io.github.t1willi.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.thymeleaf.exceptions.TemplateProcessingException;

import io.github.t1willi.http.HttpStatus;
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
     * Renders an HTML view with the specified model data and HTTP status.
     * <p>
     * This method creates a {@link ResponseEntity} with the specified HTTP status
     * and a {@link ModelView} that encapsulates the view name and data model. If
     * the model is null, an empty {@link JoltModel} is used. The response includes
     * a "Content-Type" header set to "text/html". This method is used to render
     * HTML templates populated with model data, such as in response to GET requests
     * or error scenarios. View resolution (prefix/suffix) is handled downstream.
     *
     * @param view   the name of the view template to render
     * @param model  the {@link JoltModel} containing data to populate the view, or
     *               null for an empty model
     * @param status the HTTP status code for the response (e.g., 200 OK, 404 Not
     *               Found)
     * @return a {@link ResponseEntity} with the specified status, HTML content
     *         type,
     *         and rendered view
     * @throws IllegalArgumentException    if the view name is null or empty, or if
     *                                     status is null
     * @throws TemplateProcessingException if the view cannot be rendered due to
     *                                     template errors
     * @since 1.0.1
     */
    protected ResponseEntity<ModelView> render(String view, JoltModel model, HttpStatus status) {
        if (view == null || view.isEmpty()) {
            throw new IllegalArgumentException("View name cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        JoltModel m = model != null ? model : JoltModel.empty();
        return ResponseEntity.of(status, ModelView.of(view, m))
                .contentType("text/html");
    }

    /**
     * Renders an HTML view with the specified model data and HTTP 200 OK status.
     * <p>
     * This method is a convenience wrapper for
     * {@link #render(String, JoltModel, HttpStatus)}
     * with a status of 200 OK.
     *
     * @param view  the name of the view template to render
     * @param model the {@link JoltModel} containing data to populate the view, or
     *              null for an empty model
     * @return a {@link ResponseEntity} with HTTP status 200, HTML content type, and
     *         rendered view
     * @throws IllegalArgumentException    if the view name is null or empty
     * @throws TemplateProcessingException if the view cannot be rendered due to
     *                                     template errors
     * @since 1.0.0
     */
    protected ResponseEntity<ModelView> render(String view, JoltModel model) {
        return render(view, model, HttpStatus.OK);
    }

    /**
     * Renders an HTML view.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 200 (OK)
     * but without a model.
     * That is, it is equivalent to calling {@link #render(String, JoltModel)} with
     * a null model.
     * 
     * @see #render(String, JoltModel)
     * @param view the name of the view template to render (e.g., the name of a
     *             Freemarker template file, without the .ftl extension)
     * @return a {@link ResponseEntity} with HTTP status 200, HTML content type, and
     *         ModelView as parameter.
     */
    protected ResponseEntity<ModelView> render(String view) {
        return render(view, null);
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

    /**
     * Constructs an HTTP 302 Found response to redirect the client to the specified
     * location with query parameters.
     * <p>
     * This method creates a {@link ResponseEntity} with an HTTP status of 302 Found
     * and a "Location" header set to the provided URI, appending the specified
     * query
     * parameters. It is useful for redirects that require dynamic URL construction,
     * such as passing data in the query string.
     *
     * @param location the base URI to which the client should be redirected
     * @param params   a map of query parameter names and values to append to the
     *                 URI
     * @return a {@link ResponseEntity} with HTTP status 302 and the constructed
     *         location header
     * @throws IllegalArgumentException if the location is null or empty, or if the
     *                                  params map contains null keys or values
     * @since 1.0.1
     */
    protected ResponseEntity<Void> redirect(String location, Map<String, String> params) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        if (params == null || params.containsKey(null) || params.containsValue(null)) {
            throw new IllegalArgumentException("Query parameters cannot contain null keys or values");
        }
        StringBuilder uri = new StringBuilder(location);
        if (!params.isEmpty()) {
            uri.append("?");
            String query = params.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            uri.append(query);
        }
        return ResponseEntity.redirect(uri.toString());
    }
}