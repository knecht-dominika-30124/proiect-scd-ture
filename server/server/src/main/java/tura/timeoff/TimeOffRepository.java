package tura.timeoff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimeOffRepository extends JpaRepository<TimeOffRequest, Integer> {

    List<TimeOffRequest> findAllByOrderByStartDateDesc();

    List<TimeOffRequest> findByStatusOrderByStartDateAsc(TimeOffStatus status);

    List<TimeOffRequest> findByEmployeeIdOrderByStartDateDesc(Integer employeeId);

    /** CUSTOM QUERY: are angajatul un concediu cu statusul dat care include data respectivă? */
    @Query("""
            SELECT COUNT(t) > 0 FROM TimeOffRequest t
            WHERE t.employee.id = :employeeId
              AND t.status = :status
              AND :date BETWEEN t.startDate AND t.endDate
            """)
    boolean hasTimeOffOn(@Param("employeeId") Integer employeeId,
                         @Param("date") LocalDate date,
                         @Param("status") TimeOffStatus status);
}