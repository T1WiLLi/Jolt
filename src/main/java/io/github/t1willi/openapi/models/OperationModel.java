package io.github.t1willi.openapi.models;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.ApiParameter;
import io.github.t1willi.openapi.annotations.ApiResponse;
import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;

@Getter
class OperationModel {
        private String summary;
        private String description;
        private String operationId;
        private List<String> tags;
        private List<ParameterModel> parameters;
        private RequestBodyModel requestBody;
        private Map<String, ResponseModel> responses;
        private List<Map<String, List<String>>> security;
        private boolean deprecated;

        public static OperationModel of(Docs doc, Method method, ObjectMapper mapper) {
                OperationModel model = new OperationModel();
                model.summary = doc.summary();
                model.description = doc.description();
                model.operationId = doc.operationId();
                model.tags = doc.tags().length > 0 ? List.of(doc.tags()) : null;
                model.deprecated = doc.deprecated();

                model.parameters = new ArrayList<>();
                for (ApiParameter param : doc.parameters()) {
                        ParameterModel paramModel = ParameterModel.of(param, mapper);
                        model.parameters.add(paramModel);
                }
                if (model.parameters.isEmpty()) {
                        model.parameters = null;
                }

                if (doc.requestBody() != Void.class) {
                        model.requestBody = RequestBodyModel.of(doc, mapper);
                }

                model.responses = new LinkedHashMap<>();
                for (ApiResponse resp : doc.responses()) {
                        ResponseModel respModel = ResponseModel.of(resp, method, mapper);
                        model.responses.put(String.valueOf(resp.code()), respModel);
                }

                model.security = doc.security().length > 0
                                ? List.of(Map.of(doc.security()[0], List.of()))
                                : null;

                return model;
        }
}