package com.vikrant.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String responseId;
    private String sessionId;
    private String answer;
    private long responseTimeMs;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private boolean fromCache;
    private String disclaimer = "This is an AI assistant. For critical banking decisions, please contact your branch.";
}
