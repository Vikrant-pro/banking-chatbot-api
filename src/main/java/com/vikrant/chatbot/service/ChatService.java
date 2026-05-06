package com.vikrant.chatbot.service;

import com.vikrant.chatbot.model.ChatRequest;
import com.vikrant.chatbot.model.ChatResponse;
import com.vikrant.chatbot.model.ConversationLog;
import com.vikrant.chatbot.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final LLMService llmService;
    private final ConversationRepository conversationRepository;
    private final MongoTemplate mongoTemplate;

    public ChatService(LLMService llmService, ConversationRepository conversationRepository, MongoTemplate mongoTemplate) {
        this.llmService = llmService;
        this.conversationRepository = conversationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public ChatResponse processQuestion(ChatRequest chatRequest) throws IOException {
        long startTime = System.currentTimeMillis();

        // Fetch last 5 conversation turns for context
        List<ConversationLog> conversationHistory = getConversationHistory(chatRequest.getUserId(), 5);
        List<String> historyMessages = conversationHistory.stream()
                .map(log -> "Q: " + log.getQuestion() + " A: " + log.getAnswer())
                .collect(Collectors.toList());

        // Call Claude API
        String answer = llmService.callClaudeAPI(chatRequest.getQuestion(), historyMessages);
        long responseTimeMs = System.currentTimeMillis() - startTime;

        // Detect language
        String language = detectLanguage(chatRequest.getQuestion());

        // Check if response contains sensitive information
        boolean flaggedAsSensitive = checkForSensitiveContent(answer);

        // Save conversation log to MongoDB
        ConversationLog log = new ConversationLog();
        log.setId(UUID.randomUUID().toString());
        log.setUserId(chatRequest.getUserId());
        log.setSessionId(chatRequest.getSessionId());
        log.setQuestion(chatRequest.getQuestion());
        log.setAnswer(answer);
        log.setTimestamp(LocalDateTime.now());
        log.setResponseTimeMs(responseTimeMs);
        log.setLanguage(language);
        log.setFlaggedAsSensitive(flaggedAsSensitive);

        conversationRepository.save(log);

        // Prepare response
        ChatResponse response = new ChatResponse();
        response.setResponseId(UUID.randomUUID().toString());
        response.setSessionId(chatRequest.getSessionId());
        response.setAnswer(answer);
        response.setResponseTimeMs(responseTimeMs);
        response.setTimestamp(LocalDateTime.now());

        return response;
    }

    public List<ConversationLog> getUserHistory(String userId) {
        return conversationRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    public void clearUserHistory(String userId) {
        List<ConversationLog> userLogs = conversationRepository.findByUserId(userId);
        conversationRepository.deleteAll(userLogs);
    }

    public List<ObjectNode> getPopularQuestions() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("question").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count"),
                Aggregation.limit(5),
                Aggregation.project("question", "count")
                        .and("_id").as("question")
                        .andInclude("count")
        );

        AggregationResults<ObjectNode> results = mongoTemplate.aggregate(
                aggregation,
                "conversation_logs",
                ObjectNode.class
        );

        return results.getMappedResults();
    }

    private List<ConversationLog> getConversationHistory(String userId, int limit) {
        return conversationRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String detectLanguage(String text) {
        // Simple language detection based on character ranges
        // Hindi characters are in the Devanagari Unicode range
        for (char c : text.toCharArray()) {
            if (c >= 0x0900 && c <= 0x097F) {
                return "HI";
            }
        }
        return "EN";
    }

    private boolean checkForSensitiveContent(String content) {
        String lowerContent = content.toLowerCase();
        
        // Check for common sensitive keywords
        return lowerContent.contains("otp") || 
               lowerContent.contains("pin") ||
               lowerContent.contains("password") ||
               lowerContent.contains("cvv") ||
               lowerContent.contains("sensitive") ||
               content.matches(".*\\d{16}.*"); // 16-digit card number pattern
    }
}

