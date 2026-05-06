package com.vikrant.chatbot.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ResponseCache {

    private static final int MAX_CACHE_SIZE = 20;
    private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> accessOrder = new ConcurrentLinkedQueue<>();

    public CachedResponse get(String question) {
        String normalizedQuestion = normalizeQuestion(question);
        CachedResponse cached = cache.get(normalizedQuestion);
        if (cached != null) {
            // Move to end of access order (most recently used)
            accessOrder.remove(normalizedQuestion);
            accessOrder.add(normalizedQuestion);
        }
        return cached;
    }

    public void put(String question, String answer, long responseTimeMs) {
        String normalizedQuestion = normalizeQuestion(question);

        // Remove if already exists
        cache.remove(normalizedQuestion);
        accessOrder.remove(normalizedQuestion);

        // Add new entry
        CachedResponse cachedResponse = new CachedResponse(answer, responseTimeMs, System.currentTimeMillis());
        cache.put(normalizedQuestion, cachedResponse);
        accessOrder.add(normalizedQuestion);

        // Evict oldest if cache is full
        if (cache.size() > MAX_CACHE_SIZE) {
            String oldestKey = accessOrder.poll();
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
    }

    public void clear() {
        cache.clear();
        accessOrder.clear();
    }

    public int size() {
        return cache.size();
    }

    private String normalizeQuestion(String question) {
        if (question == null) return "";
        // Normalize by converting to lowercase and removing extra whitespace
        return question.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    public static class CachedResponse {
        private final String answer;
        private final long responseTimeMs;
        private final long cachedAt;

        public CachedResponse(String answer, long responseTimeMs, long cachedAt) {
            this.answer = answer;
            this.responseTimeMs = responseTimeMs;
            this.cachedAt = cachedAt;
        }

        public String getAnswer() { return answer; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public long getCachedAt() { return cachedAt; }
    }
}

