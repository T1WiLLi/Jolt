package io.github.t1willi.localization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.utils.JacksonUtil;
import lombok.Getter;

public class LanguageConfig {
    private static final Logger logger = Logger.getLogger(LanguageConfig.class.getName());
    private static final Map<String, Map<String, Object>> LANGUAGE_CACHE = new HashMap<>();
    @Getter
    private final Map<String, Object> translations;

    public LanguageConfig(String lang) {
        this.translations = loadLanguageFile(lang);
    }

    private Map<String, Object> loadLanguageFile(String language) {
        if (language == null) {
            return null;
        }

        if (LANGUAGE_CACHE.containsKey(language)) {
            return new HashMap<>(LANGUAGE_CACHE.get(language));
        }

        ObjectMapper mapper = JacksonUtil.getObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/local/" + language + ".json")) {
            if (in == null) {
                logger.warning("Language file not found: /local/" + language + ".json");
                return null;
            }
            Map<String, Object> translations = mapper.readValue(in,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            LANGUAGE_CACHE.put(language, new HashMap<>(translations));
            return translations;
        } catch (Exception e) {
            logger.severe("Error loading language file for " + language + ": " + e.getMessage());
            return null;
        }
    }

    public String getTranslation(String key) {
        if (translations == null || key == null) {
            return null;
        }
        Object value = getNestedValue(key);
        return value instanceof String ? (String) value : null;
    }

    public List<String> getTranslations(String key) {
        if (translations == null || key == null) {
            return Collections.emptyList();
        }
        Object value = getNestedValue(key);
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            List<String> stringList = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof String) {
                    stringList.add((String) obj);
                }
            }
            return stringList;
        }
        return Collections.emptyList();
    }

    private Object getNestedValue(String key) {
        String[] keys = key.split("\\.");
        Object current = translations;
        for (String k : keys) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(k);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }
}