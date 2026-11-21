package tura.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tura.availability.Availability;
import tura.availability.AvailabilityRepository;
import tura.employee.Employee;
import tura.employee.EmployeeRepository;
import tura.employee.Role;
import tura.schedule.*;
import tura.timeoff.TimeOffRepository;
import tura.timeoff.TimeOffRequest;
import tura.timeoff.TimeOffStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Component
public class ChatContextBuilder {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AvailabilityRepository availabilityRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private ShiftAssignmentRepository assignmentRepository;
    @Autowired private ShiftAssignmentService assignmentService;
    @Autowired private TimeOffRepository timeOffRepository;

    public String build(LocalDate anyDay) {
        LocalDate monday = anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        StringBuilder sb = new StringBuilder();

        // 1. angajații, orele și preferințele lor
        sb.append("ANGAJAȚI ȘI PREFERINȚELE LOR DE PROGRAM:\n");
        for (Employee e : employeeRepository.findByRole(Role.STAFF)) {
            ShiftAssignmentService.WeekSummary week = assignmentService.weekSummary(e.getId(), monday);
            sb.append("- ").append(e.getName())
                    .append(" (").append(e.getPosition())
                    .append(", programat ").append(week.hours()).append("h din maxim ").append(week.maxHours()).append("h)")
                    .append(". Preferințe: ");
            List<Availability> prefs = availabilityRepository.findByEmployeeId(e.getId());
            if (prefs.isEmpty()) {
                sb.append("nu și-a completat preferințele");
            }
            for (Availability a : prefs) {
                sb.append(describe(a)).append("; ");
            }
            sb.append("\n");
        }

        // 2. concediile
        sb.append("\nCONCEDII APROBATE ÎN SĂPTĂMÂNA ").append(monday).append(":\n");
        for (TimeOffRequest t : timeOffRepository.findByStatusOrderByStartDateAsc(TimeOffStatus.APPROVED)) {
            if (!t.getEndDate().isBefore(monday) && !t.getStartDate().isAfter(sunday)) {
                sb.append("- ").append(t.getEmployee().getName()).append(": ")
                        .append(t.getStartDate()).append(" – ").append(t.getEndDate()).append("\n");
            }
        }
        sb.append("\nCERERI DE CONCEDIU ÎN AȘTEPTARE (de aprobat):\n");
        for (TimeOffRequest t : timeOffRepository.findByStatusOrderByStartDateAsc(TimeOffStatus.PENDING)) {
            sb.append("- ").append(t.getEmployee().getName()).append(": ")
                    .append(t.getStartDate()).append(" – ").append(t.getEndDate())
                    .append(t.getReason() != null ? " (" + t.getReason() + ")" : "").append("\n");
        }

        // 3. programul săptămânii, cu turele neacoperite
        Optional<Schedule> schedule = scheduleRepository.findByWeekStart(monday);
        if (schedule.isEmpty()) {
            sb.append("\nPROGRAM: nu există încă un program pentru săptămâna ").append(monday).append(".\n");
            return sb.toString();
        }
        sb.append("\nPROGRAMUL SĂPTĂMÂNII ").append(monday).append(" (status: ").append(schedule.get().getStatus()).append("):\n");
        for (Shift s : shiftRepository.findByScheduleIdOrderByDateAscStartTimeAsc(schedule.get().getId())) {
            List<String> names = assignmentRepository.findByShiftId(s.getId()).stream()
                    .map(sa -> sa.getEmployee().getName())
                    .toList();
            sb.append("- ").append(dayRo(s.getDate().getDayOfWeek())).append(" ").append(s.getDate())
                    .append(", ").append(s.getStartTime()).append("–").append(s.getEndTime())
                    .append(": ").append(names.size()).append("/").append(s.getRequiredStaff()).append(" oameni");
            if (!names.isEmpty()) {
                sb.append(" (").append(String.join(", ", names)).append(")");
            }
            int missing = s.getRequiredStaff() - names.size();
            if (missing > 0) {
                List<String> candidates = assignmentService.findAvailableForShift(s.getId()).stream()
                        .map(Employee::getName)
                        .toList();
                sb.append(" → LIPSESC ").append(missing)
                        .append(". Pot lucra conform preferințelor: ")
                        .append(candidates.isEmpty() ? "nimeni" : String.join(", ", candidates));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String describe(Availability a) {
        String day = dayRo(a.getDayOfWeek());
        if (!a.isAvailable()) {
            return day + " indisponibil";
        }
        if (a.getFromTime() == null && a.getToTime() == null) {
            return day + " oricând";
        }
        String text = day;
        if (a.getFromTime() != null) text += " de la " + a.getFromTime();
        if (a.getToTime() != null) text += " până la " + a.getToTime();
        return text;
    }

    private String dayRo(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "luni";
            case TUESDAY -> "marți";
            case WEDNESDAY -> "miercuri";
            case THURSDAY -> "joi";
            case FRIDAY -> "vineri";
            case SATURDAY -> "sâmbătă";
            case SUNDAY -> "duminică";
        };
    }
}