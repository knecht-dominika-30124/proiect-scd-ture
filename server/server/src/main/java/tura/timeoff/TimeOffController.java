package tura.timeoff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/timeoff")
public class TimeOffController {

    @Autowired
    private TimeOffService timeOffService;

    @GetMapping
    public List<TimeOffRequest> getAll(@RequestParam(required = false) TimeOffStatus status) {
        return timeOffService.findAll(status);
    }

    @GetMapping("/{id}")
    public TimeOffRequest getById(@PathVariable Integer id) {
        return timeOffService.findById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<TimeOffRequest> getByEmployee(@PathVariable Integer employeeId) {
        return timeOffService.findByEmployee(employeeId);
    }

    @PostMapping
    public TimeOffRequest create(@RequestBody TimeOffRequest request) {
        return timeOffService.create(request);
    }

    @PutMapping("/{id}/approve")
    public TimeOffService.ApproveResult approve(@PathVariable Integer id) {
        return timeOffService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public TimeOffRequest reject(@PathVariable Integer id) {
        return timeOffService.reject(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        timeOffService.delete(id);
    }
}