package com.vikrant.chatbot.middleware;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
                // Use a wrapper to cache the request body so it can be read multiple times
                CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(request);
                String body = wrapper.getReader().lines()
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

    // Custom request wrapper to cache the request body
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            // Cache the request body using ByteArrayOutputStream for compatibility
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            java.io.InputStream inputStream = request.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            cachedBody = baos.toByteArray();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    // Custom ServletInputStream that reads from cached bytes
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Not implemented for this use case
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }

    private static class RequestTracker {
        final java.util.List<Long> requests = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}

