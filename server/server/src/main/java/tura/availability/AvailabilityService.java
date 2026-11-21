package tura.availability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tura.employee.EmployeeService;

import java.util.List;

@Service
public class AvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private EmployeeService employeeService;

    public List<Availability> findAll() {
        return availabilityRepository.findAll();
    }

    public List<Availability> findByEmployee(Integer employeeId) {
        return availabilityRepository.findByEmployeeId(employeeId);
    }

    public Availability findById(Integer id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preferința nu există"));
    }

    public Availability create(Availability availability) {
        availability.setId(null);
        validate(availability);
        return availabilityRepository.save(availability);
    }

    public Availability update(Integer id, Availability availability) {
        findById(id);
        availability.setId(id);
        validate(availability);
        return availabilityRepository.save(availability);
    }

    public void delete(Integer id) {
        findById(id);
        availabilityRepository.deleteById(id);
    }

    private void validate(Availability a) {
        if (a.getEmployee() == null || a.getEmployee().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește angajatul");
        }
        // verificăm că angajatul există (aruncă 404 dacă nu)
        a.setEmployee(employeeService.findById(a.getEmployee().getId()));

        if (a.getDayOfWeek() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește ziua");
        }
        if (a.getFromTime() != null && a.getToTime() != null && !a.getToTime().isAfter(a.getFromTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ora de final trebuie să fie după ora de început");
        }
    }
}