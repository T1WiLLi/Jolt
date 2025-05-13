package io.github.t1willi;

import java.util.Arrays;
import java.util.List;

import io.github.t1willi.injector.annotation.Bean;

@Bean
public class UserService {
    public List<User> create() {
        return Arrays.asList(
                new User(1, "Samuel", "Arsenault", true),
                new User(1, "Oualid", "jspquoi", true),
                new User(3, "William", "Beaudin", true));
    }
}
