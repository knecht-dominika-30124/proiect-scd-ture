package tura.availability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tura.employee.EmployeeService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

@Service
public class AvailabilityService {

    public static final int MAX_DAYS_OFF_PER_MONTH = 4;
    public static final int MAX_HOUR_LIMITS_PER_WEEK = 2;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private EmployeeService employeeService;

    public List<Availability> findAll() {
        return availabilityRepository.findAll();
    }

    /** Preferințele unui angajat; dacă se dau from și to, doar din acea perioadă (ex: o lună). */
    public List<Availability> findByEmployee(Integer employeeId, LocalDate from, LocalDate to) {
        employeeService.findById(employeeId);
        if (from != null && to != null) {
            return availabilityRepository.findByEmployeeIdAndDateBetweenOrderByDate(employeeId, from, to);
        }
        return availabilityRepository.findByEmployeeIdOrderByDate(employeeId);
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

    /** Ștergerea unei preferințe = angajatul poate lucra din nou oricând în acea zi. */
    public void delete(Integer id) {
        findById(id);
        availabilityRepository.deleteById(id);
    }

    private void validate(Availability a) {
        // 1. angajatul există
        if (a.getEmployee() == null || a.getEmployee().getId() == null) {
            throw badRequest("Lipsește angajatul");
        }
        a.setEmployee(employeeService.findById(a.getEmployee().getId()));
        Integer employeeId = a.getEmployee().getId();

        // 2. data e obligatorie
        LocalDate date = a.getDate();
        if (date == null) {
            throw badRequest("Lipsește data");
        }

        // 3. o singură preferință pe zi
        boolean duplicate = (a.getId() == null)
                ? availabilityRepository.existsByEmployeeIdAndDate(employeeId, date)
                : availabilityRepository.existsByEmployeeIdAndDateAndIdNot(employeeId, date, a.getId());
        if (duplicate) {
            throw badRequest("Ai deja o preferință pentru " + date + ". Modific-o pe aceea.");
        }

        if (a.isAvailable()) {
            // ---------- restricție de oră ----------
            if (a.getFromTime() == null && a.getToTime() == null) {
                throw badRequest("Dacă poți lucra oricând în ziua asta, nu trebuie să adaugi nimic.");
            }
            if (a.getFromTime() != null && a.getToTime() != null && !a.getToTime().isAfter(a.getFromTime())) {
                throw badRequest("Ora de final trebuie să fie după ora de început");
            }

            // 4. maxim 2 restricții de oră pe săptămână (luni–duminică)
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = monday.plusDays(6);
            long count = availabilityRepository.countInPeriod(employeeId, true, monday, sunday, a.getId());
            if (count >= MAX_HOUR_LIMITS_PER_WEEK) {
                throw badRequest("Poți avea maxim " + MAX_HOUR_LIMITS_PER_WEEK
                        + " zile cu restricție de oră pe săptămână (săptămâna " + monday + " – " + sunday
                        + " are deja " + count + ").");
            }
        } else {
            // ---------- zi liberă ----------
            a.setFromTime(null);
            a.setToTime(null);

            // 5. maxim 4 zile libere pe lună
            LocalDate firstDay = date.withDayOfMonth(1);
            LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
            long count = availabilityRepository.countInPeriod(employeeId, false, firstDay, lastDay, a.getId());
            if (count >= MAX_DAYS_OFF_PER_MONTH) {
                String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.of("ro", "RO"));
                throw badRequest("Poți cere maxim " + MAX_DAYS_OFF_PER_MONTH
                        + " zile libere pe lună (în " + month + " ai deja " + count + ").");
            }
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}