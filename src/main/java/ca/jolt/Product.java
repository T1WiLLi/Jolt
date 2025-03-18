package ca.jolt;

import java.util.Date;

import ca.jolt.database.annotation.CheckEnum;
import ca.jolt.database.annotation.Column;
import ca.jolt.database.annotation.GenerationType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.Table;
import ca.jolt.database.annotation.Timestamp;
import ca.jolt.database.annotation.Timestamp.TimestampType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
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

    @Column(value = "price", nullable = false)
    private Double price;

    @CheckEnum(values = { "AVAILABLE", "UNAVAILABLE" })
    @Column(value = "status", nullable = true)
    private String status;

    @Timestamp(forUpdate = TimestampType.CREATED_AT)
    private Date createdAt;

    @Timestamp(forUpdate = TimestampType.UPDATED_AT)
    private Date updatedAt;
}
