package tura.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tura.employee.Employee;
import tura.employee.EmployeeService;
import tura.employee.Role;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Ești asistentul AI al managerului într-o aplicație de gestionare a turelor angajaților.
            Ajuți managerul să facă programul de lucru ținând cont de preferințele angajaților.
            Reguli:
            - Răspunde mereu în limba română, scurt și la obiect. Folosește liste când e cazul.
            - Folosește DOAR datele de mai jos. Nu inventa angajați, ore sau ture.
            - Când propui pe cineva pentru o tură, alege doar dintre cei care „pot lucra conform preferințelor”
              și ține cont de orele deja programate față de maximul lor.
            - Nu poți modifica programul: faci propuneri, iar managerul le aplică din aplicație.
            - Dacă întrebarea nu are legătură cu angajații, turele sau programul, spune politicos că poți ajuta doar cu programul.

            DATELE ACTUALE:
            """;

    private final RestClient ollama;
    private final String model;
    private final int contextSize;

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private EmployeeService employeeService;
    @Autowired private ChatContextBuilder contextBuilder;

    public ChatService(@Value("${ollama.url}") String url,
                       @Value("${ollama.model}") String model,
                       @Value("${ollama.context-size}") int contextSize) {
        this.ollama = RestClient.builder().baseUrl(url).build();
        this.model = model;
        this.contextSize = contextSize;
    }

    // forma răspunsului de la Ollama (luăm doar ce ne trebuie)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaResponse(OllamaMessage message) {}

    public String ask(Integer managerId, String question, LocalDate week) {
        Employee manager = employeeService.findById(managerId);
        if (manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar managerul poate folosi asistentul AI");
        }
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scrie o întrebare");
        }

        String context = contextBuilder.build(week != null ? week : LocalDate.now());

        // comandă specială: arată ce date „vede” AI-ul (util la prezentare)
        if (question.trim().equals("/context")) {
            return context;
        }

        // 1. instrucțiunile + datele actuale
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT + context));

        // 2. ultimele mesaje din conversație, în ordine cronologică
        List<ChatMessage> history = new ArrayList<>(chatMessageRepository.findTop10ByEmployeeIdOrderByIdDesc(managerId));
        Collections.reverse(history);
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.getRole() == ChatRole.USER ? "user" : "assistant", "content", m.getContent()));
        }

        // 3. întrebarea nouă
        messages.add(Map.of("role", "user", "content", question));

        String answer;
        try {
            OllamaResponse response = ollama.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "messages", messages,
                            "stream", false,
                            "options", Map.of("temperature", 0.3, "num_ctx", contextSize)))
                    .retrieve()
                    .body(OllamaResponse.class);
            answer = response.message().content();
        } catch (Exception e) {
            return "Nu am putut contacta AI-ul. Verifică dacă Ollama rulează (iconița cu lama de lângă ceas). Detalii: "
                    + e.getMessage();
        }

        save(manager, ChatRole.USER, question);
        save(manager, ChatRole.ASSISTANT, answer);
        return answer;
    }

    public List<ChatMessage> history(Integer managerId) {
        return chatMessageRepository.findByEmployeeIdOrderByIdAsc(managerId);
    }

    @Transactional
    public void clearHistory(Integer managerId) {
        chatMessageRepository.deleteByEmployeeId(managerId);
    }

    private void save(Employee manager, ChatRole role, String content) {
        ChatMessage m = new ChatMessage();
        m.setEmployee(manager);
        m.setRole(role);
        m.setContent(content);
        chatMessageRepository.save(m);
    }
}