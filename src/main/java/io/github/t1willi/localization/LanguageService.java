package io.github.t1willi.localization;

import java.util.Map;
import java.util.logging.Logger;

import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.template.JoltModel;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Bean
public class LanguageService {
    private static final Logger logger = Logger.getLogger(LanguageService.class.getName());

    private static String lang;
    @Getter
    private static LanguageConfig languageConfig;
    @Getter
    private static JoltModel globalLanguageModel;

    public LanguageService() {
        lang = ConfigurationManager.getInstance().getProperty("server.defaultLanguage", null);
        if (lang == null) {
            logger.fine("No default language defined. If you want to use localization, add a language file " +
                    "to the /locale directory in the /resources directory (e.g., /locale/en.json) and set " +
                    "'server.defaultLanguage' in your configuration.");
        }
        initializeLanguage(lang);
    }

    @PostConstruct
    public void init() {
        if (globalLanguageModel == null) {
            initializeLanguage(lang);
        }
    }

    private static void initializeLanguage(String langToUse) {
        if (langToUse == null) {
            languageConfig = null;
            globalLanguageModel = JoltModel.empty();
        } else {
            languageConfig = new LanguageConfig(langToUse);
            Map<String, Object> translations = languageConfig.getTranslations();
            if (translations == null) {
                logger.severe("Failed to load translations for language: " + langToUse +
                        ". Check that /locale/" + langToUse + ".json exists and is valid.");
                globalLanguageModel = JoltModel.empty();
            } else {
                translations.put("languageCode", langToUse);
                globalLanguageModel = JoltModel.of(Map.of("lang", translations));
            }
        }
        lang = langToUse;
    }

    public static void changeLanguage(String langToUse) {
        if (langToUse == null || langToUse.trim().isEmpty()) {
            throw new IllegalArgumentException("Language cannot be null or empty when changing language explicitly");
        }
        initializeLanguage(langToUse);
    }

    public static String getCurrentLanguage() {
        return lang;
    }

    public static boolean isLocalizationEnabled() {
        return lang != null && globalLanguageModel != null && !globalLanguageModel.asMap().isEmpty();
    }
}