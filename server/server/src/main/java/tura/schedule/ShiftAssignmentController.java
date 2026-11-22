package tura.schedule;
import tura.employee.Employee;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assignment")
public class ShiftAssignmentController {

    @Autowired
    private ShiftAssignmentService assignmentService;

    public record AssignRequest(Integer shiftId, Integer employeeId) {}

    @GetMapping("/shift/{shiftId}")
    public List<ShiftAssignment> getByShift(@PathVariable Integer shiftId) {
        return assignmentService.findByShift(shiftId);
    }

    @PostMapping
    public ShiftAssignment assign(@RequestBody AssignRequest request) {
        return assignmentService.assign(request.shiftId(), request.employeeId());
    }

    @DeleteMapping("/{id}")
    public void unassign(@PathVariable Integer id) {
        assignmentService.unassign(id);
    }

    @GetMapping("/shift/{shiftId}/available")
    public List<Employee> getAvailableForShift(@PathVariable Integer shiftId) {
        return assignmentService.findAvailableForShift(shiftId);
    }

    @GetMapping("/employee/{employeeId}/week")
    public ShiftAssignmentService.WeekSummary getWeek(@PathVariable Integer employeeId,
                                                      @RequestParam LocalDate date) {
        return assignmentService.weekSummary(employeeId, date);
    }

    @GetMapping("/schedule/{scheduleId}")
    public List<ShiftAssignment> getBySchedule(@PathVariable Integer scheduleId) {
        return assignmentService.findBySchedule(scheduleId);
    }

    /** ex: /assignment/employee/7/shifts?from=2026-12-01&to=2026-12-31 */
    @GetMapping("/employee/{employeeId}/shifts")
    public List<Shift> getPublishedShifts(@PathVariable Integer employeeId,
                                          @RequestParam LocalDate from,
                                          @RequestParam LocalDate to) {
        return assignmentService.publishedShiftsForEmployee(employeeId, from, to);
    }
}