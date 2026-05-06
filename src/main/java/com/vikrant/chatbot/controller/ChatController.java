package com.vikrant.chatbot.controller;

import com.vikrant.chatbot.model.ChatRequest;
import com.vikrant.chatbot.model.ChatResponse;
import com.vikrant.chatbot.model.ConversationLog;
import com.vikrant.chatbot.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /api/chat/ask
     * Ask a banking question and get AI-powered response
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody ChatRequest chatRequest) {
        try {
            // Validate input
            if (chatRequest.getUserId() == null || chatRequest.getUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "userId is required"));
            }
            // sessionId is now optional - will be auto-generated if not provided
            if (chatRequest.getQuestion() == null || chatRequest.getQuestion().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "question is required"));
            }

            ChatResponse response = chatService.processQuestion(chatRequest);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process question: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * GET /api/chat/history/{userId}
     * Get conversation history for a user (last 10 conversations)
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getHistory(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "userId is required"));
            }

            List<ConversationLog> history = chatService.getUserHistory(userId);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve history: " + e.getMessage()));
        }
    }

    /**
     * GET /api/chat/popular-questions
     * Get top 5 most asked questions
     */
    @GetMapping("/popular-questions")
    public ResponseEntity<?> getPopularQuestions() {
        try {
            List<ObjectNode> popularQuestions = chatService.getPopularQuestions();
            return ResponseEntity.ok(popularQuestions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve popular questions: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/chat/history/{userId}
     * Clear conversation history for a user
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<?> deleteHistory(@PathVariable String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "userId is required"));
            }

            chatService.clearUserHistory(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation history cleared for user " + userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete history: " + e.getMessage()));
        }
    }
}
