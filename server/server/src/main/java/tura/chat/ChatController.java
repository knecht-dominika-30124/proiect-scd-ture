package tura.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    public record ChatRequest(Integer managerId, String message, LocalDate week) {}

    public record ChatReply(String reply) {}

    @PostMapping
    public ChatReply ask(@RequestBody ChatRequest request) {
        return new ChatReply(chatService.ask(request.managerId(), request.message(), request.week()));
    }

    @GetMapping("/{managerId}/history")
    public List<ChatMessage> history(@PathVariable Integer managerId) {
        return chatService.history(managerId);
    }

    @DeleteMapping("/{managerId}/history")
    public void clearHistory(@PathVariable Integer managerId) {
        chatService.clearHistory(managerId);
    }
}