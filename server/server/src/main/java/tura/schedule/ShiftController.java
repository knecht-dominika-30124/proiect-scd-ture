package tura.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shift")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @GetMapping("/schedule/{scheduleId}")
    public List<Shift> getBySchedule(@PathVariable Integer scheduleId) {
        return shiftService.findBySchedule(scheduleId);
    }

    @GetMapping("/{id}")
    public Shift getById(@PathVariable Integer id) {
        return shiftService.findById(id);
    }

    @PostMapping
    public Shift create(@RequestBody Shift shift) {
        return shiftService.create(shift);
    }

    @PutMapping("/{id}")
    public Shift update(@PathVariable Integer id, @RequestBody Shift shift) {
        return shiftService.update(id, shift);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        shiftService.delete(id);
    }
}