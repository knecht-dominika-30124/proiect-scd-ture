package tura.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tura.employee.Employee;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Integer> {

    List<Availability> findByEmployeeId(Integer employeeId);

    Optional<Availability> findByEmployeeIdAndDayOfWeek(Integer employeeId, DayOfWeek dayOfWeek);

    boolean existsByEmployeeIdAndDayOfWeek(Integer employeeId, DayOfWeek dayOfWeek);

    boolean existsByEmployeeIdAndDayOfWeekAndIdNot(Integer employeeId, DayOfWeek dayOfWeek, Integer id);

    /** CUSTOM QUERY: angajații care, conform preferințelor, pot lucra toată tura dată. */
    @Query("""
            SELECT a.employee FROM Availability a
            WHERE a.dayOfWeek = :day
              AND a.available = true
              AND (a.fromTime IS NULL OR a.fromTime <= :start)
              AND (a.toTime IS NULL OR a.toTime >= :end)
            ORDER BY a.employee.name
            """)
    List<Employee> findAvailableFor(@Param("day") DayOfWeek day,
                                    @Param("start") LocalTime start,
                                    @Param("end") LocalTime end);
}