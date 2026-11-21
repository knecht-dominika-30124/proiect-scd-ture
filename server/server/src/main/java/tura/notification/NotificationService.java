package tura.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tura.employee.Employee;
import tura.employee.EmployeeRepository;
import tura.employee.Role;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /** Conexiunile deschise: pentru fiecare id de angajat, lista lui de conexiuni. */
    private final Map<Integer, List<SseEmitter>> connections = new ConcurrentHashMap<>();

    // ---------- conexiunea permanentă (SSE) ----------

    public SseEmitter subscribe(Integer employeeId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = conexiunea nu expiră niciodată
        connections.computeIfAbsent(employeeId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        // când conexiunea se închide (browser închis, eroare), o scoatem din listă
        Runnable remove = () -> connections.getOrDefault(employeeId, List.of()).remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("Conectat la notificări"));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    // ---------- trimiterea notificărilor ----------

    public Notification send(Employee employee, String message) {
        Notification notification = new Notification();
        notification.setEmployee(employee);
        notification.setMessage(message);
        notification = notificationRepository.save(notification);   // 1. o salvăm

        for (SseEmitter emitter : connections.getOrDefault(employee.getId(), List.of())) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification)); // 2. o trimitem live
            } catch (IOException e) {
                connections.get(employee.getId()).remove(emitter);
            }
        }
        return notification;
    }

    public void sendToManagers(String message) {
        for (Employee manager : employeeRepository.findByRole(Role.MANAGER)) {
            send(manager, message);
        }
    }

    // ---------- citire ----------

    public List<Notification> findByEmployee(Integer employeeId) {
        return notificationRepository.findTop30ByEmployeeIdOrderByCreatedOnDesc(employeeId);
    }

    public long countUnread(Integer employeeId) {
        return notificationRepository.countByEmployeeIdAndSeenFalse(employeeId);
    }

    public Notification markSeen(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificarea nu există"));
        notification.setSeen(true);
        return notificationRepository.save(notification);
    }
}