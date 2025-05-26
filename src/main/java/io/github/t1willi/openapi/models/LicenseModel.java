package io.github.t1willi.openapi.models;

@SuppressWarnings("unused")
public final class LicenseModel {
    private String name;
    private String url;

    public static LicenseModel of(String n, String u) {
        LicenseModel l = new LicenseModel();
        l.name = n.isEmpty() ? null : n;
        l.url = u.isEmpty() ? null : u;
        return l;
    }
}
