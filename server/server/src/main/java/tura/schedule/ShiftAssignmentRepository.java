package tura.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Integer> {

    List<ShiftAssignment> findByShiftId(Integer shiftId);

    boolean existsByShiftIdAndEmployeeId(Integer shiftId, Integer employeeId);

    long countByShiftId(Integer shiftId);

    List<ShiftAssignment> findByShiftScheduleId(Integer scheduleId);

    /** CUSTOM QUERY: turele unui angajat între două date, în ordine cronologică. */
    @Query("""
            SELECT sa FROM ShiftAssignment sa
            WHERE sa.employee.id = :employeeId
              AND sa.shift.date BETWEEN :from AND :to
            ORDER BY sa.shift.date, sa.shift.startTime
            """)
    List<ShiftAssignment> findForEmployeeBetween(@Param("employeeId") Integer employeeId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

    /** CUSTOM QUERY: turele unui angajat între două date, doar din programele cu statusul dat. */
    @Query("""
            SELECT sa FROM ShiftAssignment sa
            WHERE sa.employee.id = :employeeId
              AND sa.shift.date BETWEEN :from AND :to
              AND sa.shift.schedule.status = :status
            ORDER BY sa.shift.date, sa.shift.startTime
            """)
    List<ShiftAssignment> findForEmployeeBetweenWithStatus(@Param("employeeId") Integer employeeId,
                                                           @Param("from") LocalDate from,
                                                           @Param("to") LocalDate to,
                                                           @Param("status") ScheduleStatus status);
}