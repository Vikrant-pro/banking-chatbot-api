# Banking Chatbot API - Postman Collection Usage Guide

## 📋 Collection Overview

This Postman collection provides comprehensive testing for your Banking Chatbot API with security, audit, and caching features.

**File:** `Banking_Chatbot_API_Postman_Collection.json`

---

## 🚀 How to Import

### Method 1: Direct Import
1. Open Postman
2. Click **Import** button
3. Select **File**
4. Choose `Banking_Chatbot_API_Postman_Collection.json`
5. Click **Import**

### Method 2: Copy-Paste JSON
1. Open Postman
2. Click **Import** button
3. Select **Raw text** tab
4. Copy the entire JSON content from the file
5. Click **Import**

---

## 📊 Test Cases Included

### 1. **Basic Chat Question**
- **Endpoint:** `POST /api/chat/ask`
- **Test:** Home loan interest rate question
- **Validates:** Response structure, required fields, session management

### 2. **EMI Calculation**
- **Endpoint:** `POST /api/chat/ask`
- **Test:** Complex EMI calculation request
- **Validates:** Mathematical responses, session continuity

### 3. **Sensitive Question Test**
- **Endpoint:** `POST /api/chat/ask`
- **Test:** OTP-related security question
- **Validates:** Security response, no Claude API call, fast response

### 4. **Hindi Language Test**
- **Endpoint:** `POST /api/chat/ask`
- **Test:** Account opening question in Hindi
- **Validates:** Multi-language support, new session creation

### 5. **Conversation History**
- **Endpoint:** `GET /api/chat/history/user123`
- **Test:** Retrieve user's conversation history
- **Validates:** Array response, field structure, timestamp ordering

### 6. **Popular Questions**
- **Endpoint:** `GET /api/chat/popular-questions`
- **Test:** Get top 5 most asked questions
- **Validates:** Aggregation results, count ordering, max 5 items

### 7. **Rate Limiting Test**
- **Endpoints:** `POST /api/chat/ask` (11 requests)
- **Test:** Same user making 11 rapid requests
- **Validates:** First 10 succeed (200), 11th fails (429)

### 8. **Health Check**
- **Endpoint:** `GET /actuator/health`
- **Test:** Application health status
- **Validates:** UP status, MongoDB connectivity, disk space

---

## 🧪 Test Assertions

Each request includes comprehensive test assertions:

### Common Validations
- ✅ HTTP status codes
- ✅ Response JSON structure
- ✅ Required field presence
- ✅ Data type validation
- ✅ Business logic validation

### Security Features Tested
- ✅ Sensitive content detection
- ✅ Rate limiting enforcement
- ✅ Session management
- ✅ Response caching indicators

### Performance Validations
- ✅ Response time ranges
- ✅ Cache hit indicators
- ✅ Fast security responses

---

## 🎯 How to Run Tests

### Prerequisites
1. **Start MongoDB:** `docker-compose up -d`
2. **Set API Key:** `$env:ANTHROPIC_API_KEY = "your-key"`
3. **Start Application:** `mvn spring-boot:run`
4. **Verify Health:** Test case 8 should pass

### Running Individual Tests
1. Select any request in the collection
2. Click **Send**
3. View **Test Results** tab for assertions

### Running Rate Limit Test
1. Run requests 7.1 through 7.10 sequentially (all should pass with 200)
2. Run request 7.11 (should fail with 429)
3. Wait 1 minute for rate limit reset

### Running Full Collection
1. Click **Runner** button in Postman
2. Select the collection
3. Click **Run**
4. Review results summary

---

## 📈 Expected Results

### ✅ Successful Tests
- **Status:** All tests pass (green)
- **Response Times:** 50-2000ms (cached responses faster)
- **Rate Limiting:** 10/10 pass, 11th fails appropriately

### 🔍 Response Validation
```json
// Standard Response Structure
{
  "responseId": "uuid",
  "sessionId": "sess001",
  "answer": "AI response...",
  "responseTimeMs": 1250,
  "timestamp": "2025-05-06T10:30:00",
  "fromCache": false,
  "disclaimer": "This is an AI assistant..."
}
```

### 🚨 Error Responses
```json
// Rate Limited (429)
{
  "error": "Rate limit exceeded. Try after 1 minute."
}

// Security Response (200)
{
  "responseId": "uuid",
  "sessionId": "sess001",
  "answer": "For security reasons, never share OTP...",
  "responseTimeMs": 50,
  "timestamp": "2025-05-06T10:30:00",
  "fromCache": false,
  "disclaimer": "This is an AI assistant..."
}
```

---

## 🔧 Customization

### Change Base URL
```json
"variable": [
  {
    "key": "baseUrl",
    "value": "http://your-server:8081",
    "type": "string"
  }
]
```

### Add New Test Cases
1. Copy an existing request structure
2. Modify name, request body, and test assertions
3. Add to the `item` array

### Environment Variables
Create a Postman environment with:
- `baseUrl`: `http://localhost:8081`
- `userId`: `test_user`
- `sessionId`: `test_session`

---

## 📊 Test Coverage

| Feature | Test Case | Validation |
|---------|-----------|------------|
| Claude AI Integration | 1, 2, 4 | Response content, timing |
| Security Detection | 3 | Sensitive content handling |
| Session Management | 1, 2, 4 | Session ID consistency |
| Response Caching | 1 (repeat) | Cache hit indicators |
| Rate Limiting | 7.1-7.11 | 10 req/min enforcement |
| Conversation History | 5 | Data persistence, ordering |
| Analytics | 6 | Aggregation queries |
| Health Monitoring | 8 | System status |
| Multi-language | 4 | Hindi support |
| Error Handling | All | Proper HTTP codes |

---

## 🚨 Troubleshooting

### Tests Failing?
1. **Check API Status:** Run health check test first
2. **Verify MongoDB:** Ensure `docker-compose up -d` is running
3. **Check API Key:** Ensure `ANTHROPIC_API_KEY` is set
4. **Review Logs:** Check application console for errors

### Rate Limit Not Working?
- Rate limits reset every minute
- Each test uses different userId to avoid conflicts
- Wait 60 seconds between rate limit test runs

### Cache Not Working?
- Cache is in-memory (resets on restart)
- Same question text triggers cache hit
- Check `fromCache: true` in response

---

## 📋 Collection Metadata

- **Format:** Postman Collection v2.1
- **Requests:** 18 (including 11 rate limit tests)
- **Tests:** 100+ individual assertions
- **Coverage:** All API endpoints + security features
- **Compatibility:** Postman v8.0+

---

## 🎉 Ready to Test!

**Import the collection and start testing your Banking Chatbot API!**

The collection provides enterprise-grade testing coverage for:
- ✅ API functionality
- ✅ Security features
- ✅ Performance validation
- ✅ Error handling
- ✅ Business logic verification

**Happy Testing! 🚀**
