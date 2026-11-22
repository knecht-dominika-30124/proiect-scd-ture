package tura.availability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @GetMapping
    public List<Availability> getAll() {
        return availabilityService.findAll();
    }

    @GetMapping("/{id}")
    public Availability getById(@PathVariable Integer id) {
        return availabilityService.findById(id);
    }

    /** ex: /availability/employee/3?from=2026-12-01&to=2026-12-31 */
    @GetMapping("/employee/{employeeId}")
    public List<Availability> getByEmployee(@PathVariable Integer employeeId,
                                            @RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to) {
        return availabilityService.findByEmployee(employeeId, from, to);
    }

    @PostMapping
    public Availability create(@RequestBody Availability availability) {
        return availabilityService.create(availability);
    }

    @PutMapping("/{id}")
    public Availability update(@PathVariable Integer id, @RequestBody Availability availability) {
        return availabilityService.update(id, availability);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        availabilityService.delete(id);
    }
}