package io.github.t1willi.openapi.models;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Getter
public final class ContactModel {
    private String name;
    private String email;
    private String url;

    public static ContactModel of(String n, String e, String u) {
        if (n == null || e == null || u == null) {
            return null;
        }
        ContactModel c = new ContactModel();
        c.name = n.isEmpty() ? null : n;
        c.email = e.isEmpty() ? null : e;
        c.url = u.isEmpty() ? null : u;
        return c;
    }
}
