package io.github.t1willi.security.nonce;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import java.util.List;
import java.util.logging.Logger;

/**
 * A FreeMarker directive that provides the nonce() method for templates.
 * This allows templates to call ${nonce()} to get a CSP nonce for inline
 * scripts.
 */
public class NonceDirective implements TemplateMethodModelEx {
    private static final Logger logger = Logger.getLogger(NonceDirective.class.getName());

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
        if (!arguments.isEmpty()) {
            throw new TemplateModelException("nonce() method does not accept arguments");
        }

        String nonce = Nonce.get();
        if (nonce == null) {
            logger.warning("Failed to generate nonce for template");
            throw new TemplateModelException("Failed to generate nonce for template");
        }

        return new SimpleScalar(nonce);
    }
}
