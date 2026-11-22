package tura.availability;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import tura.employee.Employee;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * O excepție de la program, pe o dată anume:
 *  - available = false              → zi liberă
 *  - available = true + fromTime/toTime → poate lucra doar în acel interval
 * Dacă pentru o dată nu există nimic, angajatul poate lucra oricând.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    private boolean available;

    private LocalTime fromTime;

    private LocalTime toTime;

    private String note;
}