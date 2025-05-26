package io.github.t1willi.openapi.models;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class SecurityRequirementModel {
    private Map<String, List<String>> requirements;

    public static SecurityRequirementModel of(String scheme) {
        if (scheme.isEmpty()) {
            return null;
        }
        SecurityRequirementModel model = new SecurityRequirementModel();
        model.requirements = Map.of(scheme, List.of());
        return model;
    }
}