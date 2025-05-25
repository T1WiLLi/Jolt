package io.github.t1willi.http;

import io.github.t1willi.template.JoltModel;
import lombok.Getter;

/**
 * A final class that encapsulates a view name and a data model for rendering in
 * an MVC framework.
 * <p>
 * The {@code ModelView} class pairs a view name (typically corresponding to a
 * template file) with a
 * {@link JoltModel} containing data to be rendered in the view. It is designed
 * for use in web
 * applications following the Model-View-Controller (MVC) pattern, where
 * controllers return a
 * {@code ModelView} instance to specify which template to render and the data
 * to populate it. The
 * class is immutable and provides a static factory method for creation. If a
 * null model is provided,
 * an empty {@link JoltModel} is used to ensure safe rendering.
 *
 * @since 1.0.0
 */
@Getter
public final class ModelView {
    private final String view;
    private final JoltModel model;

    /**
     * Constructs a new ModelView with the specified view name and model.
     *
     * @param view  the name of the view template to render
     * @param model the data model to populate the view
     */
    private ModelView(String view, JoltModel model) {
        this.view = view;
        this.model = model != null ? model : JoltModel.empty();
    }

    /**
     * Creates a new ModelView instance with the specified view name and model.
     * <p>
     * This static factory method constructs a {@code ModelView} instance that pairs
     * the given view
     * name with a {@link JoltModel}. The view name typically corresponds to a
     * template file (e.g., an
     * HTML template) used for rendering in an MVC framework. If the provided model
     * is null, an empty
     * {@link JoltModel} is used to prevent null pointer issues during rendering.
     * This method is the
     * primary way to create {@code ModelView} instances for use in controllers.
     *
     * @param view  the name of the view template to render (e.g., the name of an
     *              HTML template file)
     * @param model the {@link JoltModel} containing data to populate the view, or
     *              null for an empty model
     * @return a new {@code ModelView} instance
     * @throws IllegalArgumentException if the view name is null or empty
     * @since 1.0.0
     */
    public static ModelView of(String view, JoltModel model) {
        return new ModelView(view, model);
    }
}