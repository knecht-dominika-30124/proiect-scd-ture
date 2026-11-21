package tura.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ScheduleService scheduleService;

    public List<Shift> findBySchedule(Integer scheduleId) {
        scheduleService.findById(scheduleId); // 404 dacă programul nu există
        return shiftRepository.findByScheduleIdOrderByDateAscStartTimeAsc(scheduleId);
    }

    public Shift findById(Integer id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tura nu există"));
    }

    public Shift create(Shift shift) {
        shift.setId(null);
        validate(shift);
        return shiftRepository.save(shift);
    }

    public Shift update(Integer id, Shift shift) {
        findById(id);
        shift.setId(id);
        validate(shift);
        return shiftRepository.save(shift);
    }

    public void delete(Integer id) {
        findById(id);
        shiftRepository.deleteById(id);
    }

    private void validate(Shift shift) {
        // 1. programul trebuie să existe
        if (shift.getSchedule() == null || shift.getSchedule().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește programul");
        }
        Schedule schedule = scheduleService.findById(shift.getSchedule().getId());
        shift.setSchedule(schedule);

        // 2. data și orele sunt obligatorii
        if (shift.getDate() == null || shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completați data, ora de început și ora de final");
        }

        // 3. tura trebuie să fie în săptămâna programului (luni–duminică)
        LocalDate monday = schedule.getWeekStart();
        LocalDate sunday = monday.plusDays(6);
        if (shift.getDate().isBefore(monday) || shift.getDate().isAfter(sunday)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data trebuie să fie între " + monday + " și " + sunday);
        }

        // 4. ora de final după ora de început
        if (!shift.getEndTime().isAfter(shift.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ora de final trebuie să fie după ora de început");
        }

        // 5. măcar o persoană pe tură (dacă nu se trimite nimic, punem 1)
        if (shift.getRequiredStaff() == null || shift.getRequiredStaff() < 1) {
            shift.setRequiredStaff(1);
        }
    }
}