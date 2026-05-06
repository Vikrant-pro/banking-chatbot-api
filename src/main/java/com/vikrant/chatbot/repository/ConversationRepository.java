package com.vikrant.chatbot.repository;

import com.vikrant.chatbot.model.ConversationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<ConversationLog, String> {
    
    List<ConversationLog> findByUserId(String userId);
    
    List<ConversationLog> findByUserIdOrderByTimestampDesc(String userId);
    
    List<ConversationLog> findByUserIdAndSessionIdOrderByTimestampDesc(String userId, String sessionId);
}
