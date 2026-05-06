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
    private final SessionManager sessionManager;
    private final ResponseCache responseCache;

    private static final String SENSITIVE_RESPONSE = "For security reasons, never share OTP, PIN or card details with anyone including bank staff. If someone asked you for this, please call 1800-XXX-XXXX immediately.";

    public ChatService(LLMService llmService, ConversationRepository conversationRepository,
                      MongoTemplate mongoTemplate, SessionManager sessionManager, ResponseCache responseCache) {
        this.llmService = llmService;
        this.conversationRepository = conversationRepository;
        this.mongoTemplate = mongoTemplate;
        this.sessionManager = sessionManager;
        this.responseCache = responseCache;
    }

    public ChatResponse processQuestion(ChatRequest chatRequest) throws IOException {
        long startTime = System.currentTimeMillis();

        // Handle session management
        String sessionId = sessionManager.getOrCreateSessionId(chatRequest.getSessionId(), chatRequest.getUserId());
        chatRequest.setSessionId(sessionId);

        // Check for sensitive content in the question
        if (isSensitiveQuestion(chatRequest.getQuestion())) {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            // Save flagged conversation log
            ConversationLog log = new ConversationLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(chatRequest.getUserId());
            log.setSessionId(sessionId);
            log.setQuestion(chatRequest.getQuestion());
            log.setAnswer(SENSITIVE_RESPONSE);
            log.setTimestamp(LocalDateTime.now());
            log.setResponseTimeMs(responseTimeMs);
            log.setLanguage(detectLanguage(chatRequest.getQuestion()));
            log.setFlaggedAsSensitive(true);

            conversationRepository.save(log);

            // Return security response
            ChatResponse response = new ChatResponse();
            response.setResponseId(UUID.randomUUID().toString());
            response.setSessionId(sessionId);
            response.setAnswer(SENSITIVE_RESPONSE);
            response.setResponseTimeMs(responseTimeMs);
            response.setTimestamp(LocalDateTime.now());
            response.setFromCache(false);

            return response;
        }

        // Check cache first
        ResponseCache.CachedResponse cachedResponse = responseCache.get(chatRequest.getQuestion());
        if (cachedResponse != null) {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            // Save conversation log for cached response
            ConversationLog log = new ConversationLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(chatRequest.getUserId());
            log.setSessionId(sessionId);
            log.setQuestion(chatRequest.getQuestion());
            log.setAnswer(cachedResponse.getAnswer());
            log.setTimestamp(LocalDateTime.now());
            log.setResponseTimeMs(responseTimeMs);
            log.setLanguage(detectLanguage(chatRequest.getQuestion()));
            log.setFlaggedAsSensitive(false);

            conversationRepository.save(log);

            // Return cached response
            ChatResponse response = new ChatResponse();
            response.setResponseId(UUID.randomUUID().toString());
            response.setSessionId(sessionId);
            response.setAnswer(cachedResponse.getAnswer());
            response.setResponseTimeMs(responseTimeMs);
            response.setTimestamp(LocalDateTime.now());
            response.setFromCache(true);

            return response;
        }

        // Fetch conversation history for context (only if session is valid)
        List<ConversationLog> conversationHistory = List.of();
        if (sessionManager.isSessionValid(sessionId)) {
            conversationHistory = getConversationHistory(chatRequest.getUserId(), sessionId, 5);
        }

        List<String> historyMessages = conversationHistory.stream()
                .map(log -> "Q: " + log.getQuestion() + " A: " + log.getAnswer())
                .collect(Collectors.toList());

        // Call Claude API
        String answer = llmService.callClaudeAPI(chatRequest.getQuestion(), historyMessages);
        long responseTimeMs = System.currentTimeMillis() - startTime;

        // Cache the response
        responseCache.put(chatRequest.getQuestion(), answer, responseTimeMs);

        // Detect language
        String language = detectLanguage(chatRequest.getQuestion());

        // Check if response contains sensitive information
        boolean flaggedAsSensitive = checkForSensitiveContent(answer);

        // Save conversation log to MongoDB
        ConversationLog log = new ConversationLog();
        log.setId(UUID.randomUUID().toString());
        log.setUserId(chatRequest.getUserId());
        log.setSessionId(sessionId);
        log.setQuestion(chatRequest.getQuestion());
        log.setAnswer(answer);
        log.setTimestamp(LocalDateTime.now());
        log.setResponseTimeMs(responseTimeMs);
        log.setLanguage(language);
        log.setFlaggedAsSensitive(flaggedAsSensitive);

        conversationRepository.save(log);

        // Update session activity
        sessionManager.updateSessionActivity(sessionId);

        // Prepare response
        ChatResponse response = new ChatResponse();
        response.setResponseId(UUID.randomUUID().toString());
        response.setSessionId(sessionId);
        response.setAnswer(answer);
        response.setResponseTimeMs(responseTimeMs);
        response.setTimestamp(LocalDateTime.now());
        response.setFromCache(false);

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

    private List<ConversationLog> getConversationHistory(String userId, String sessionId, int limit) {
        return conversationRepository.findByUserIdAndSessionIdOrderByTimestampDesc(userId, sessionId)
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

    private boolean isSensitiveQuestion(String question) {
        String lowerQuestion = question.toLowerCase();
        return lowerQuestion.contains("otp") || lowerQuestion.contains("pin") || lowerQuestion.contains("password");
    }
}
