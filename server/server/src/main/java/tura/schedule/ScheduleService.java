package tura.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tura.employee.Employee;
import tura.notification.NotificationService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private NotificationService notificationService;

    // ---------- citire ----------

    public List<Schedule> findAll() {
        return scheduleRepository.findAllByOrderByWeekStartDesc();
    }

    public Schedule findById(Integer id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programul nu există"));
    }

    /** Găsește programul săptămânii în care se află data primită (orice zi din săptămână). */
    public Schedule findByWeek(LocalDate anyDay) {
        LocalDate monday = toMonday(anyDay);
        return scheduleRepository.findByWeekStart(monday)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nu există program pentru săptămâna " + monday));
    }

    // ---------- modificări ----------

    public Schedule create(Schedule schedule) {
        if (schedule.getWeekStart() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește săptămâna");
        }
        LocalDate monday = toMonday(schedule.getWeekStart());
        if (scheduleRepository.existsByWeekStart(monday)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Există deja un program pentru săptămâna " + monday);
        }
        schedule.setId(null);
        schedule.setWeekStart(monday);
        schedule.setStatus(ScheduleStatus.DRAFT);
        return scheduleRepository.save(schedule);
    }

    public void delete(Integer id) {
        findById(id);
        scheduleRepository.deleteById(id);
    }

    /** Publică programul: angajații îl pot vedea și fiecare primește o notificare cu turele lui. */
    public Schedule publish(Integer id) {
        Schedule schedule = findById(id);
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programul este deja publicat");
        }
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        scheduleRepository.save(schedule);

        // grupăm alocările pe angajat: Andrei → [tura 1, tura 4], Ioana → [tura 2]
        Map<Employee, List<Shift>> shiftsByEmployee = new HashMap<>();
        for (ShiftAssignment sa : assignmentRepository.findByShiftScheduleId(id)) {
            shiftsByEmployee.computeIfAbsent(sa.getEmployee(), e -> new ArrayList<>()).add(sa.getShift());
        }

        for (Map.Entry<Employee, List<Shift>> entry : shiftsByEmployee.entrySet()) {
            List<Shift> shifts = entry.getValue();
            double hours = shifts.stream().mapToDouble(Shift::getHours).sum();
            notificationService.send(entry.getKey(), "Programul pentru săptămâna " + schedule.getWeekStart()
                    + " a fost publicat: ai " + shifts.size() + " ture (" + hours + " ore).");
        }
        return schedule;
    }

    // ---------- ajutătoare ----------

    /** Transformă orice dată în ziua de luni a săptămânii ei. */
    private LocalDate toMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}