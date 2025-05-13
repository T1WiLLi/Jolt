package io.github.t1willi;

import io.github.t1willi.annotations.MapField;
import io.github.t1willi.annotations.MapTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@MapTo(UserDTO.class)
public class User {

    @MapField("name")
    private String username;

    private String email;

    private String password;
}
