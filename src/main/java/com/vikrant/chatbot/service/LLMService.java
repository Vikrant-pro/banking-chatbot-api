package com.vikrant.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class LLMService {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private String systemPrompt;

    public LLMService(ResourceLoader resourceLoader) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.resourceLoader = resourceLoader;
        loadSystemPrompt();
    }

    private void loadSystemPrompt() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/banking-system-prompt.txt");
            systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            // Fallback to default prompt if file not found
            systemPrompt = getDefaultBankingPrompt();
        }
    }

    public String callClaudeAPI(String userQuestion, List<String> conversationHistory) throws IOException {
        long startTime = System.currentTimeMillis();

        // Build messages array
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // Add conversation history
        for (String historyMessage : conversationHistory) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", "user");
            msgNode.put("content", historyMessage);
            messagesArray.add(msgNode);

            // Add a dummy assistant response to maintain history format
            ObjectNode assistantNode = objectMapper.createObjectNode();
            assistantNode.put("role", "assistant");
            assistantNode.put("content", "Previous response");
            messagesArray.add(assistantNode);
        }

        // Add current question as the last user message
        ObjectNode currentQuestion = objectMapper.createObjectNode();
        currentQuestion.put("role", "user");
        currentQuestion.put("content", userQuestion);
        messagesArray.add(currentQuestion);

        // Build request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1024);
        requestBody.put("system", systemPrompt);
        requestBody.set("messages", messagesArray);

        // Make API call
        Request request = new Request.Builder()
                .url(CLAUDE_API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        try (Response httpResponse = httpClient.newCall(request).execute()) {
            if (!httpResponse.isSuccessful()) {
                throw new IOException("Claude API error: " + httpResponse.code() + " " + httpResponse.body().string());
            }

            String responseBody = httpResponse.body().string();
            JsonNode responseNode = objectMapper.readTree(responseBody);

            // Extract the response text from Claude's response
            if (responseNode.has("content") && responseNode.get("content").isArray()) {
                JsonNode contentArray = responseNode.get("content");
                if (contentArray.size() > 0) {
                    return contentArray.get(0).get("text").asText();
                }
            }

            throw new IOException("Unexpected Claude API response format");
        }
    }

    private String getDefaultBankingPrompt() {
        return """
                You are a helpful banking assistant for an Indian retail bank.
                You assist customers with:
                - Savings and current account queries
                - Loan information (home loan, personal loan, car loan)
                - Credit card queries and disputes
                - Fixed deposit and recurring deposit information
                - Transaction disputes and fraud reporting
                - Interest rates and EMI calculations
                - KYC and account opening process
                - RBI regulations and compliance queries
                
                Rules you must follow:
                - Never share or ask for sensitive data like OTP, PIN, full card number
                - Always recommend calling 1800-XXX-XXXX for urgent fraud cases
                - If you don't know something, say: "Please visit your nearest branch for assistance"
                - Respond in the same language the customer uses (English or Hindi)
                - Keep responses concise and helpful
                - For EMI calculations, show the formula and calculate clearly
                """;
    }

    public long measureResponseTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}

