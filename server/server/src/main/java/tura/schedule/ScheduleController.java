package tura.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping
    public List<Schedule> getAll() {
        return scheduleService.findAll();
    }

    @GetMapping("/{id}")
    public Schedule getById(@PathVariable Integer id) {
        return scheduleService.findById(id);
    }

    @GetMapping("/week")
    public Schedule getByWeek(@RequestParam LocalDate date) {
        return scheduleService.findByWeek(date);
    }

    @PostMapping
    public Schedule create(@RequestBody Schedule schedule) {
        return scheduleService.create(schedule);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        scheduleService.delete(id);
    }

    @PutMapping("/{id}/publish")
    public Schedule publish(@PathVariable Integer id) {
        return scheduleService.publish(id);
    }
}