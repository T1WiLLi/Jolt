package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.t1willi.openapi.annotations.OpenApi;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Getter
public final class InfoModel {
    private String title;
    private String version;
    private String description;
    @JsonIgnore
    private String path;
    private String termsOfService;
    private ContactModel contact;
    private LicenseModel license;

    public static InfoModel of(OpenApi openApi) {
        InfoModel i = new InfoModel();
        i.title = openApi.title();
        i.version = openApi.version();
        i.description = openApi.description().isEmpty() ? null : openApi.description();
        i.path = openApi.path().isEmpty() ? null : openApi.path();
        i.termsOfService = openApi.termsOfService().isEmpty() ? null : openApi.termsOfService();
        i.contact = ContactModel.of(openApi.contactName(), openApi.contactEmail(), openApi.contactUrl());
        i.license = LicenseModel.of(openApi.licenseName(), openApi.licenseUrl());
        return i;
    }
}
