package tura.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tura.employee.Employee;
import tura.employee.Role;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Integer> {

    List<Availability> findByEmployeeIdOrderByDate(Integer employeeId);

    List<Availability> findByEmployeeIdAndDateBetweenOrderByDate(Integer employeeId, LocalDate from, LocalDate to);

    Optional<Availability> findByEmployeeIdAndDate(Integer employeeId, LocalDate date);

    boolean existsByEmployeeIdAndDate(Integer employeeId, LocalDate date);

    boolean existsByEmployeeIdAndDateAndIdNot(Integer employeeId, LocalDate date, Integer id);

    /** CUSTOM QUERY: câte zile libere (available=false) sau restricții de oră (available=true) are angajatul într-o perioadă. */
    @Query("""
            SELECT COUNT(a) FROM Availability a
            WHERE a.employee.id = :employeeId
              AND a.available = :available
              AND a.date BETWEEN :from AND :to
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    long countInPeriod(@Param("employeeId") Integer employeeId,
                       @Param("available") boolean available,
                       @Param("from") LocalDate from,
                       @Param("to") LocalDate to,
                       @Param("excludeId") Integer excludeId);

    /**
     * CUSTOM QUERY: angajații care pot lucra toată tura dată.
     * Un angajat e bun dacă NU are, în acea dată, o preferință care îl blochează.
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE e.role = :role
              AND NOT EXISTS (
                  SELECT a FROM Availability a
                  WHERE a.employee = e
                    AND a.date = :date
                    AND (a.available = false
                         OR (a.fromTime IS NOT NULL AND a.fromTime > :start)
                         OR (a.toTime IS NOT NULL AND a.toTime < :end))
              )
            ORDER BY e.name
            """)
    List<Employee> findAvailableFor(@Param("date") LocalDate date,
                                    @Param("start") LocalTime start,
                                    @Param("end") LocalTime end,
                                    @Param("role") Role role);
}