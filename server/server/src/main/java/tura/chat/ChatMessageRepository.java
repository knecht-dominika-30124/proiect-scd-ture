package tura.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findTop10ByEmployeeIdOrderByIdDesc(Integer employeeId);

    List<ChatMessage> findByEmployeeIdOrderByIdAsc(Integer employeeId);

    void deleteByEmployeeId(Integer employeeId);
}