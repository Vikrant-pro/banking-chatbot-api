package com.vikrant.chatbot.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterFilter implements Filter {

    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW_MS = 60 * 1000; // 1 minute
    private final ConcurrentHashMap<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String userId = extractUserIdFromRequest(httpRequest);
            
            if (userId != null) {
                if (isRateLimited(userId)) {
                    sendRateLimitResponse(httpResponse);
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }

    private String extractUserIdFromRequest(HttpServletRequest request) throws IOException {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                String body = request.getReader().lines()
                        .reduce("", (acc, actual) -> acc + actual);
                
                if (!body.isEmpty()) {
                    try {
                        var jsonNode = objectMapper.readTree(body);
                        if (jsonNode.has("userId")) {
                            return jsonNode.get("userId").asText();
                        }
                    } catch (Exception e) {
                        // Silently ignore parsing errors
                    }
                }
            }
        } catch (Exception e) {
            // Continue without rate limiting if userId extraction fails
        }
        return null;
    }

    private boolean isRateLimited(String userId) {
        long currentTime = System.currentTimeMillis();
        
        requestTrackers.computeIfPresent(userId, (key, tracker) -> {
            // Remove old requests outside the time window
            tracker.requests.removeIf(timestamp -> currentTime - timestamp > TIME_WINDOW_MS);
            return tracker;
        });
        
        RequestTracker tracker = requestTrackers.computeIfAbsent(userId, k -> new RequestTracker());
        
        if (tracker.requests.size() >= MAX_REQUESTS) {
            return true;
        }
        
        tracker.requests.add(currentTime);
        return false;
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Rate limit exceeded. Try after 1 minute.\"}");
    }

    private static class RequestTracker {
        final java.util.List<Long> requests = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}

