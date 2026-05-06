package com.vikrant.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversation_logs")
public class ConversationLog {
    @Id
    private String id;
    
    private String userId;
    private String sessionId;
    private String question;
    private String answer;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private long responseTimeMs;
    private String language; // EN or HI
    private boolean flaggedAsSensitive;
}

