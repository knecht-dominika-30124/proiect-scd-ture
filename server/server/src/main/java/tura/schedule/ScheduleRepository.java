package tura.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    Optional<Schedule> findByWeekStart(LocalDate weekStart);

    boolean existsByWeekStart(LocalDate weekStart);

    List<Schedule> findAllByOrderByWeekStartDesc();


}