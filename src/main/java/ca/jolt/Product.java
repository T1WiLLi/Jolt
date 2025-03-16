package ca.jolt;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Table(table = "product", unique = { "name" }, indexes = { "name" })
@Getter
@Setter
public class Product {
    @Id(generationType = GenerationType.IDENTITY)
    private Integer id;

    @Column(value = "name", length = 255, nullable = false)
    private String name;

    @Column(value = "description", nullable = true)
    private String description;

    @JsonIgnore
    @Column(value = "price", nullable = false)
    private Double price;
}
