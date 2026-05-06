package com.vikrant.chatbot.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManager {

    private static final long SESSION_TIMEOUT_MINUTES = 30;
    private final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public String getOrCreateSessionId(String providedSessionId, String userId) {
        if (providedSessionId != null && !providedSessionId.trim().isEmpty()) {
            // Check if session exists and is still valid
            SessionInfo sessionInfo = activeSessions.get(providedSessionId);
            if (sessionInfo != null && !isSessionExpired(sessionInfo)) {
                // Update last activity
                sessionInfo.setLastActivity(LocalDateTime.now());
                return providedSessionId;
            }
        }

        // Create new session
        String newSessionId = UUID.randomUUID().toString();
        SessionInfo newSession = new SessionInfo(userId, LocalDateTime.now(), LocalDateTime.now());
        activeSessions.put(newSessionId, newSession);

        // Clean up expired sessions periodically
        cleanupExpiredSessions();

        return newSessionId;
    }

    public boolean isSessionValid(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        return sessionInfo != null && !isSessionExpired(sessionInfo);
    }

    public void updateSessionActivity(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo != null) {
            sessionInfo.setLastActivity(LocalDateTime.now());
        }
    }

    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    private boolean isSessionExpired(SessionInfo sessionInfo) {
        return sessionInfo.getLastActivity().isBefore(
            LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES)
        );
    }

    private void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> isSessionExpired(entry.getValue()));
    }

    private static class SessionInfo {
        private final String userId;
        private final LocalDateTime createdAt;
        private LocalDateTime lastActivity;

        public SessionInfo(String userId, LocalDateTime createdAt, LocalDateTime lastActivity) {
            this.userId = userId;
            this.createdAt = createdAt;
            this.lastActivity = lastActivity;
        }

        public String getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }
}

