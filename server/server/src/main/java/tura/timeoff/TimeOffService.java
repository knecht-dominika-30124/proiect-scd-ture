package tura.timeoff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tura.employee.EmployeeService;
import tura.notification.NotificationService;
import tura.schedule.ShiftAssignment;
import tura.schedule.ShiftAssignmentRepository;

import java.util.List;

@Service
public class TimeOffService {

    @Autowired
    private TimeOffRepository timeOffRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private NotificationService notificationService;

    public record ApproveResult(TimeOffRequest request, int removedFromShifts) {}

    // ---------- citire ----------

    public List<TimeOffRequest> findAll(TimeOffStatus status) {
        if (status == null) {
            return timeOffRepository.findAllByOrderByStartDateDesc();
        }
        return timeOffRepository.findByStatusOrderByStartDateAsc(status);
    }

    public List<TimeOffRequest> findByEmployee(Integer employeeId) {
        employeeService.findById(employeeId);
        return timeOffRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
    }

    public TimeOffRequest findById(Integer id) {
        return timeOffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu există"));
    }

    // ---------- angajatul cere liber ----------

    public TimeOffRequest create(TimeOffRequest request) {
        if (request.getEmployee() == null || request.getEmployee().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește angajatul");
        }
        request.setEmployee(employeeService.findById(request.getEmployee().getId()));

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completați data de început și de final");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de final nu poate fi înainte de cea de început");
        }

        request.setId(null);
        request.setStatus(TimeOffStatus.PENDING); // orice cerere nouă începe „în așteptare”
        TimeOffRequest saved = timeOffRepository.save(request);

        notificationService.sendToManagers(saved.getEmployee().getName() + " a cerut liber "
                + saved.getStartDate() + " – " + saved.getEndDate()
                + (saved.getReason() != null ? " (" + saved.getReason() + ")" : ""));
        return saved;
    }

    // ---------- managerul decide ----------

    /** Aprobă cererea și scoate angajatul din turele în care era pus în acele zile. */
    @Transactional
    public ApproveResult approve(Integer id) {
        TimeOffRequest request = findPending(id);
        request.setStatus(TimeOffStatus.APPROVED);
        timeOffRepository.save(request);

        List<ShiftAssignment> conflicts = assignmentRepository.findForEmployeeBetween(
                request.getEmployee().getId(), request.getStartDate(), request.getEndDate());
        assignmentRepository.deleteAll(conflicts);

        notificationService.send(request.getEmployee(), "Cererea ta de liber "
                + request.getStartDate() + " – " + request.getEndDate() + " a fost aprobată ✅");

        return new ApproveResult(request, conflicts.size());
    }

    public TimeOffRequest reject(Integer id) {
        TimeOffRequest request = findPending(id);
        request.setStatus(TimeOffStatus.REJECTED);
        TimeOffRequest saved = timeOffRepository.save(request);

        notificationService.send(saved.getEmployee(), "Cererea ta de liber "
                + saved.getStartDate() + " – " + saved.getEndDate() + " a fost respinsă ❌");
        return saved;
    }

    /** Angajatul își poate retrage cererea doar cât timp e încă în așteptare. */
    public void delete(Integer id) {
        TimeOffRequest request = findPending(id);
        timeOffRepository.delete(request);
    }

    // ---------- ajutătoare ----------

    private TimeOffRequest findPending(Integer id) {
        TimeOffRequest request = findById(id);
        if (request.getStatus() != TimeOffStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cererea a fost deja procesată (" + request.getStatus() + ")");
        }
        return request;
    }
}