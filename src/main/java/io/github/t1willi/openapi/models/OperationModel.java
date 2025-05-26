package io.github.t1willi.openapi.models;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;

@Getter
public final class OperationModel {
        private String summary;
        private String description;
        private String operationId;
        private List<String> tags;
        private List<ParameterModel> parameters;
        private RequestBodyModel requestBody;
        private Map<String, ReponseModel> responses;
        private List<SecurityRequirementModel> security;
        private boolean deprecated;

        public static OperationModel of(Docs docs, ObjectMapper mapper) {
                OperationModel model = new OperationModel();
                model.summary = docs.summary();
                model.description = docs.description().isEmpty() ? null : docs.description();
                model.operationId = docs.operationId().isEmpty() ? null : docs.operationId();
                model.tags = docs.tags().length > 0 ? Arrays.asList(docs.tags()) : null;
                model.parameters = docs.parameters().length > 0
                                ? Arrays.stream(docs.parameters())
                                                .map(param -> ParameterModel.of(param, mapper))
                                                .collect(Collectors.toList())
                                : null;
                model.requestBody = RequestBodyModel.of(docs, mapper);
                model.responses = Arrays.stream(docs.responses())
                                .collect(Collectors.toMap(
                                                r -> String.valueOf(r.code()),
                                                r -> ReponseModel.of(r, mapper),
                                                (r1, r2) -> r1,
                                                LinkedHashMap::new));
                model.security = docs.security().length > 0
                                ? Arrays.stream(docs.security())
                                                .map(SecurityRequirementModel::of)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toList())
                                : null;
                model.deprecated = docs.deprecated();
                return model;
        }
}
