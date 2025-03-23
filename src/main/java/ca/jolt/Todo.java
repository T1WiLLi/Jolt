package ca.jolt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Todo {
    private int id;
    private String text;
    private boolean completed;
    private String description;
    private String date;
    private String username;

    public Todo(int id, String text, boolean completed, String description, String date, String username) {
        this.id = id;
        this.text = text;
        this.completed = completed;
        this.description = description;
        this.date = date;
        this.username = username;
    }
}