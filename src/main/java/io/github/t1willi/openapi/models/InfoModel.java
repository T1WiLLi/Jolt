package io.github.t1willi.openapi.models;

import io.github.t1willi.openapi.annotations.OpenApi;
import lombok.Getter;

@Getter
public final class InfoModel {
    private String title;
    private String version;
    private String description;
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
        i.contact = ContactModel.of(openApi.contactName(), openApi.contactUrl(), openApi.contactEmail());
        i.license = LicenseModel.of(openApi.licenseName(), openApi.licenseUrl());
        return i;
    }
}
