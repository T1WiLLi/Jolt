package ca.jolt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Todo {
    private int id;
    private String text;
    private boolean completed;

    public Todo(int id, String text, boolean completed) {
        this.id = id;
        this.text = text;
        this.completed = completed;
    }
}
