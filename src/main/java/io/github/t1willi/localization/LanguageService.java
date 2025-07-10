package io.github.t1willi.localization;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.template.JoltModel;

@Bean
public class LanguageService {
    private static final Logger logger = Logger.getLogger(LanguageService.class.getName());
    private static String defaultLang;

    public LanguageService() {
        LanguageService.defaultLang = ConfigurationManager.getInstance().getProperty("server.defaultLanguage", null);
        if (defaultLang == null || defaultLang.isBlank()) {
            logger.fine("No default language defined; localization disabled until a cookie is set");
        } else {
            logger.fine("Default language is: " + defaultLang);
        }
    }

    /**
     * @return the language code for the current request: the "_language" cookie if
     *         present,
     *         otherwise, the default from config (may be null!)
     */
    public static String getCurrentLanguage() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        return ctx.cookieValue("_language").orElse(defaultLang);
    }

    /**
     * Change the language for subsequent requests by setting the "_language"
     * cookie.
     * 
     * @param newLang the new language code (e.g. "en", "fr")
     */
    public static void changeLanguage(String newLang) {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        ctx.addCookie()
                .setName("_language")
                .setValue(newLang)
                .httpOnly(false)
                .path("/")
                .maxAge(60 * 60 * 24 * 30)
                .sameSite("Lax")
                .build();
    }

    public static JoltModel forCurrentRequest() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();

        Optional<String> cookieLang = getCookieLanguage();

        emitDefaultCookieIfNeeded(ctx, cookieLang);

        String lang = cookieLang.orElse(defaultLang);
        return buildLanguageModel(lang);
    }

    private static Optional<String> getCookieLanguage() {
        return JoltDispatcherServlet.getCurrentContext().cookieValue("_language");
    }

    private static void emitDefaultCookieIfNeeded(JoltContext ctx, Optional<String> cookieLang) {
        if (cookieLang.isEmpty()
                && defaultLang != null
                && !defaultLang.isBlank()) {
            changeLanguage(defaultLang);
        }
    }

    private static JoltModel buildLanguageModel(String lang) {
        if (lang == null || lang.isBlank()) {
            return JoltModel.empty();
        }

        Map<String, Object> translations = new LanguageConfig(lang).getTranslations();
        if (translations == null || translations.isEmpty()) {
            logger.warning("Localization file missing or empty for: " + lang);
            return JoltModel.empty();
        }

        translations.put("languageCode", lang);
        return JoltModel.of(Map.of("lang", translations));
    }
}