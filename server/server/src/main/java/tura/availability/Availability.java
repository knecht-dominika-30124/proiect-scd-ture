package tura.availability;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tura.employee.Employee;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "day_of_week"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private boolean available;

    private LocalTime fromTime;

    private LocalTime toTime;

    private String note;
}