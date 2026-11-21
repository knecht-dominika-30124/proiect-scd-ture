package tura.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tura.availability.Availability;
import tura.availability.AvailabilityRepository;
import tura.employee.Employee;
import tura.employee.EmployeeService;
import tura.timeoff.TimeOffRepository;
import tura.timeoff.TimeOffStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftAssignmentService {

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private TimeOffRepository timeOffRepository;

    public record WeekSummary(String employee, LocalDate weekStart, double hours, int maxHours, List<Shift> shifts) {}

    public List<ShiftAssignment> findByShift(Integer shiftId) {
        shiftService.findById(shiftId);
        return assignmentRepository.findByShiftId(shiftId);
    }

    /** Angajații care pot lucra în tură conform preferințelor și nu sunt deja în ea. */
    public List<Employee> findAvailableForShift(Integer shiftId) {
        Shift shift = shiftService.findById(shiftId);

        List<Employee> available = availabilityRepository.findAvailableFor(
                shift.getDate().getDayOfWeek(),
                shift.getStartTime(),
                shift.getEndTime());

        return available.stream()
                .filter(e -> !assignmentRepository.existsByShiftIdAndEmployeeId(shiftId, e.getId()))
                .toList();
    }

    /** Turele și totalul de ore ale unui angajat în săptămâna care conține data dată. */
    public WeekSummary weekSummary(Integer employeeId, LocalDate anyDay) {
        Employee employee = employeeService.findById(employeeId);
        LocalDate monday = anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<Shift> shifts = assignmentRepository.findForEmployeeBetween(employeeId, monday, sunday)
                .stream()
                .map(ShiftAssignment::getShift)
                .toList();

        double hours = shifts.stream().mapToDouble(Shift::getHours).sum();
        int maxHours = employee.getMaxHoursPerWeek() == null ? 40 : employee.getMaxHoursPerWeek();

        return new WeekSummary(employee.getName(), monday, hours, maxHours, shifts);
    }

    public ShiftAssignment assign(Integer shiftId, Integer employeeId) {
        Shift shift = shiftService.findById(shiftId);
        Employee employee = employeeService.findById(employeeId);

        // 1. nu de două ori în aceeași tură
        if (assignmentRepository.existsByShiftIdAndEmployeeId(shiftId, employeeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    employee.getName() + " este deja în această tură");
        }

        // 2. tura nu trebuie să fie deja completă
        long alreadyAssigned = assignmentRepository.countByShiftId(shiftId);
        if (alreadyAssigned >= shift.getRequiredStaff()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tura este deja completă (" + alreadyAssigned + "/" + shift.getRequiredStaff() + ")");
        }

        // 3. tura trebuie să se potrivească cu preferința angajatului
        String problem = checkPreference(employee, shift);
        if (problem != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    employee.getName() + " nu poate lucra în această tură: " + problem);
        }

        // 6. nu poate lucra dacă are concediu aprobat în ziua aceea
        if (timeOffRepository.hasTimeOffOn(employeeId, shift.getDate(), TimeOffStatus.APPROVED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    employee.getName() + " are concediu aprobat pe " + shift.getDate());
        }

        // ce ture are deja angajatul în săptămâna asta
        WeekSummary week = weekSummary(employeeId, shift.getDate());

        // 4. nu poate fi în două ture care se suprapun
        for (Shift other : week.shifts()) {
            boolean sameDay = other.getDate().equals(shift.getDate());
            boolean overlaps = other.getStartTime().isBefore(shift.getEndTime())
                    && shift.getStartTime().isBefore(other.getEndTime());
            if (sameDay && overlaps) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        employee.getName() + " lucrează deja " + other.getStartTime() + "–" + other.getEndTime()
                                + " în aceeași zi");
            }
        }

        // 5. nu poate depăși limita de ore pe săptămână
        double totalHours = week.hours() + shift.getHours();
        if (totalHours > week.maxHours()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    employee.getName() + " ar ajunge la " + totalHours + "h, peste limita de "
                            + week.maxHours() + "h pe săptămână");
        }

        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShift(shift);
        assignment.setEmployee(employee);
        return assignmentRepository.save(assignment);
    }

    public void unassign(Integer assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocarea nu există");
        }
        assignmentRepository.deleteById(assignmentId);
    }

    /**
     * Verifică dacă tura se potrivește cu preferința angajatului din ziua respectivă.
     * Întoarce null dacă e totul în regulă, altfel motivul pentru care nu se potrivește.
     */
    private String checkPreference(Employee employee, Shift shift) {
        DayOfWeek day = shift.getDate().getDayOfWeek();
        Optional<Availability> preference =
                availabilityRepository.findByEmployeeIdAndDayOfWeek(employee.getId(), day);

        if (preference.isEmpty()) {
            return "nu și-a completat preferința pentru " + day;
        }

        Availability p = preference.get();
        if (!p.isAvailable()) {
            return "a marcat " + day + " ca zi în care nu poate lucra";
        }
        if (p.getFromTime() != null && p.getFromTime().isAfter(shift.getStartTime())) {
            return day + " poate lucra doar de la " + p.getFromTime();
        }
        if (p.getToTime() != null && p.getToTime().isBefore(shift.getEndTime())) {
            return day + " poate lucra doar până la " + p.getToTime();
        }
        return null;
    }
}