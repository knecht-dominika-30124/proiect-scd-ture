package tura.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import tura.employee.Employee;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "employee_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Shift shift;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;
}