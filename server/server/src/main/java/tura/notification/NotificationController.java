package tura.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/employee/{employeeId}")
    public List<Notification> getForEmployee(@PathVariable Integer employeeId) {
        return notificationService.findByEmployee(employeeId);
    }

    @GetMapping("/employee/{employeeId}/unread-count")
    public long unreadCount(@PathVariable Integer employeeId) {
        return notificationService.countUnread(employeeId);
    }

    @PutMapping("/{id}/seen")
    public Notification markSeen(@PathVariable Integer id) {
        return notificationService.markSeen(id);
    }

    /** Conexiunea permanentă: serverul trimite notificările în timp real. */
    @GetMapping(value = "/stream/{employeeId}", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream(@PathVariable Integer employeeId) {
        return notificationService.subscribe(employeeId);
    }

}