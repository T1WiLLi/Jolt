package io.github.t1willi;

import io.github.t1willi.annotations.MapTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@MapTo(UserDTO.class)
public class User {
    private int id;
    private String firstname;
    private String lastname;
    private boolean isBeau;
}