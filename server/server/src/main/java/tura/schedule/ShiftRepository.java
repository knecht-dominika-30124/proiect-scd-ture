package tura.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {

    List<Shift> findByScheduleIdOrderByDateAscStartTimeAsc(Integer scheduleId);
}