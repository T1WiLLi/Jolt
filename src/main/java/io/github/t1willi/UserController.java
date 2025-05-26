package io.github.t1willi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.t1willi.annotations.Body;
import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.core.ApiController;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.openapi.annotations.ApiParameter;
import io.github.t1willi.openapi.annotations.ApiResponse;
import io.github.t1willi.openapi.annotations.Docs;
import lombok.AllArgsConstructor;

@Controller("[controller]s")
public class UserController extends ApiController {

    private List<User> users = new ArrayList<>(Arrays.asList(
            new User(1, "William Beaudin", "williambeaudun@gmail.com", "2004-11-07", 20),
            new User(2, "John Doe", "johndoe@example.com", "1990-01-01", 35),
            new User(3, "Jane Doe", "janedoe@example.com", "1995-06-15", 30),
            new User(4, "John Smith", "johnsmith@example.com", "1980-03-20", 45),
            new User(5, "Jane Smith", "janesmith@example.com", "1990-09-10", 34)));

    @AllArgsConstructor
    public static class User {
        public int id;
        public String name;
        public String email;
        public String dob;
        public int age;
    }

    public static class CreateUserDto {
        public String name;
        public String email;
        public String dob;
    }

    @Get
    @Docs(summary = "Get all users", description = "Get all users in the system", operationId = "getUser", tags = {
            "users" }, responses = {
                    @ApiResponse(code = 200, description = "A list of users", schema = User[].class),
            })
    public List<User> getUsers() {
        return users;
    }

    @Get("{user_id}")
    @Docs(summary = "Get user by id", description = "Get user by id", operationId = "getUserById", tags = {
            "users" }, parameters = {
                    @ApiParameter(name = "user_id", in = ApiParameter.In.PATH, description = "The id of the user", required = true, type = Integer.class, example = "1")
            }, responses = {
                    @ApiResponse(code = 302, description = "User found", schema = User.class),
                    @ApiResponse(code = 404, description = "User not found")
            })
    public User getUserById(@Path("user_id") int id) {
        return users.stream()
                .filter(user -> user.id == id)
                .findFirst()
                .orElse(null);
    }

    @Post
    @Docs(summary = "Create a new user", description = "Create a new user with the provided details", operationId = "createUser", tags = {
            "users" }, requestBody = CreateUserDto.class, requestDescription = "User creation data (name, email, date of birth)", responses = {
                    @ApiResponse(code = 201, description = "The created user", schema = User.class),
                    @ApiResponse(code = 400, description = "Invalid request")
            })
    public ResponseEntity<?> createUser(@Body CreateUserDto dto) {
        if (dto.name == null || dto.email == null || dto.dob == null) {
            return badRequest("Invalid dto, missing required fields: name, email, dob");
        }

        User user = new User(users.size() + 1, dto.name, dto.email, dto.dob, calculateAge(dto.dob));
        users.add(user);
        return okJson(user);
    }

    private int calculateAge(String dob) {
        try {
            int birthYear = Integer.parseInt(dob.split("-")[0]);
            int currentYear = 2025;
            return currentYear - birthYear;
        } catch (Exception e) {
            return 30;
        }
    }
}
