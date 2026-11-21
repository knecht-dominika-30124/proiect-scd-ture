package tura.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop30ByEmployeeIdOrderByCreatedOnDesc(Integer employeeId);

    long countByEmployeeIdAndSeenFalse(Integer employeeId);
}