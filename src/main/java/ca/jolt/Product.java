package ca.jolt;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Product {
    private Integer id;
    private String name;
    private String description;
    private double price;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
