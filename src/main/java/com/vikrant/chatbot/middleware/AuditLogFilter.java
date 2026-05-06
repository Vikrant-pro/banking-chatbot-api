package com.vikrant.chatbot.middleware;

import com.vikrant.chatbot.model.AuditLog;
import com.vikrant.chatbot.repository.AuditLogRepository;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLogFilter implements Filter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogFilter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

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

            long startTime = System.currentTimeMillis();
            String requestId = UUID.randomUUID().toString();

            // Extract userId from request body if it's a POST to /api/chat/ask
            String userId = extractUserIdFromRequest(httpRequest);
            String endpoint = httpRequest.getRequestURI();
            String ipAddress = getClientIpAddress(httpRequest);
            String httpMethod = httpRequest.getMethod();

            // Log the incoming request
            AuditLog requestLog = new AuditLog();
            requestLog.setId(UUID.randomUUID().toString());
            requestLog.setRequestId(requestId);
            requestLog.setUserId(userId);
            requestLog.setEndpoint(endpoint);
            requestLog.setIpAddress(ipAddress);
            requestLog.setHttpMethod(httpMethod);
            requestLog.setTimestamp(LocalDateTime.now());

            auditLogRepository.save(requestLog);

            // Create a custom response wrapper to capture the status code
            StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper(httpResponse);

            try {
                chain.doFilter(request, responseWrapper);
            } finally {
                // Log the response
                long responseTimeMs = System.currentTimeMillis() - startTime;
                int statusCode = responseWrapper.getStatus();

                AuditLog responseLog = new AuditLog();
                responseLog.setId(UUID.randomUUID().toString());
                responseLog.setRequestId(requestId);
                responseLog.setUserId(userId);
                responseLog.setEndpoint(endpoint);
                responseLog.setIpAddress(ipAddress);
                responseLog.setHttpMethod(httpMethod);
                responseLog.setStatusCode(statusCode);
                responseLog.setResponseTimeMs(responseTimeMs);
                responseLog.setTimestamp(LocalDateTime.now());

                auditLogRepository.save(responseLog);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }

    private String extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // Use a wrapper to cache the request body so it can be read multiple times
                CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(request);
                String body = wrapper.getReader().lines()
                        .reduce("", (acc, actual) -> acc + actual);

                if (!body.isEmpty()) {
                    try {
                        var jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                        if (jsonNode.has("userId")) {
                            return jsonNode.get("userId").asText();
                        }
                    } catch (Exception e) {
                        // Silently ignore parsing errors
                    }
                }
            }
        } catch (Exception e) {
            // Continue without userId if extraction fails
        }
        return "anonymous";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
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

    // Custom response wrapper to capture status code
    private static class StatusCaptureResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private int status = 200;

        public StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        public int getStatus() {
            return status;
        }
    }
}
