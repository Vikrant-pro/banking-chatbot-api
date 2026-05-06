# Banking Chatbot API

A Spring Boot REST API for a Banking FAQ Chatbot with Claude AI integration. This project demonstrates a production-ready chatbot API with MongoDB persistence, rate limiting, and Anthropic Claude API integration.

## Features

- **AI-Powered Responses**: Integrates with Anthropic Claude API (claude-sonnet-4-20250514) for intelligent banking queries
- **Multi-Language Support**: Supports both English and Hindi responses
- **Conversation History**: Stores and retrieves conversation logs from MongoDB
- **Rate Limiting**: Implements 10 requests per minute per user limit
- **Popular Questions Analytics**: Tracks and returns top 5 most asked questions
- **Health Checks**: Built-in Spring Boot Actuator health endpoint
- **Banking-Specific Prompts**: Custom system prompts for banking domain expertise

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MongoDB 7.0
- **API Client**: OkHttp3
- **Build Tool**: Maven
- **Additional**: Lombok, Jackson

## Project Structure

```
banking-chatbot-api/
├── pom.xml
├── docker-compose.yml
├── README.md
├── src/main/java/com/vikrant/chatbot/
│   ├── BankingChatbotApplication.java
│   ├── config/
│   │   └── AppConfig.java
│   ├── controller/
│   │   └── ChatController.java
│   ├── service/
│   │   ├── ChatService.java
│   │   └── LLMService.java
│   ├── model/
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   └── ConversationLog.java
│   ├── repository/
│   │   └── ConversationRepository.java
│   └── middleware/
│       └── RateLimiterFilter.java
└── src/main/resources/
    ├── application.yml
    └── prompts/
        └── banking-system-prompt.txt
```

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- Anthropic API Key

## Setup Instructions

### 1. Clone and Navigate

```bash
cd banking-chatbot-api
```

### 2. Start MongoDB

```bash
docker-compose up -d
```

### 3. Configure API Key

Set your Anthropic API key as an environment variable:

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY = "your-anthropic-key-here"
```

**Linux/Mac (Bash):**
```bash
export ANTHROPIC_API_KEY="your-anthropic-key-here"
```

Or update `application.yml` directly:
```yaml
anthropic:
  api:
    key: your-anthropic-key-here
```

### 4. Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

The API will start on `http://localhost:8081`

## API Endpoints

### 1. Ask a Question
**POST** `/api/chat/ask`

Request Body:
```json
{
  "userId": "user123",
  "sessionId": "session456",
  "question": "What are the interest rates for savings account?"
}
```

Response:
```json
{
  "responseId": "resp_uuid",
  "sessionId": "session456",
  "answer": "Our savings account offers competitive interest rates...",
  "responseTimeMs": 1250,
  "timestamp": "2025-05-06T10:30:00"
}
```

### 2. Get Conversation History
**GET** `/api/chat/history/{userId}`

Response:
```json
[
  {
    "id": "log_uuid",
    "userId": "user123",
    "sessionId": "session456",
    "question": "What is the question?",
    "answer": "This is the answer...",
    "timestamp": "2025-05-06T10:30:00",
    "responseTimeMs": 1250,
    "language": "EN",
    "flaggedAsSensitive": false
  }
]
```

### 3. Get Popular Questions
**GET** `/api/chat/popular-questions`

Response:
```json
[
  {
    "question": "How to open an account?",
    "count": 45
  },
  {
    "question": "What is the interest rate?",
    "count": 38
  }
]
```

### 4. Delete Conversation History
**DELETE** `/api/chat/history/{userId}`

Response:
```json
{
  "success": true,
  "message": "Conversation history cleared for user user123"
}
```

### 5. Health Check
**GET** `/actuator/health`

Response:
```json
{
  "status": "UP"
}
```

## Rate Limiting

- **Limit**: 10 requests per minute per userId
- **Status Code**: 429 Too Many Requests
- **Response**:
```json
{
  "error": "Rate limit exceeded. Try after 1 minute."
}
```

## Banking Domain Expertise

The chatbot is specifically trained to handle:
- Savings and current account queries
- Loan information (home, personal, car)
- Credit card queries and disputes
- Fixed and recurring deposits
- Transaction disputes and fraud reporting
- Interest rates and EMI calculations
- KYC and account opening
- RBI regulations and compliance

## Security Rules Implemented

- ✅ Never shares OTP, PIN, or full card numbers
- ✅ Recommends fraud hotline for urgent cases
- ✅ Directs to nearest branch for unsupported queries
- ✅ Multi-language responses (EN/HI)
- ✅ Rate limiting per user
- ✅ Conversation history tracking

## Development Notes

- **Conversation Context**: Last 5 conversation turns are included in Claude API requests for context
- **Response Tracking**: All responses are tracked with response time metrics
- **Sensitive Data Flagging**: Responses are flagged if they contain sensitive information
- **Async Processing**: Consider adding async processing for high-volume scenarios

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ANTHROPIC_API_KEY` | Anthropic API Key | Required |
| `SERVER_PORT` | Server Port | 8081 |
| `MONGODB_URI` | MongoDB Connection URI | mongodb://localhost:27017/chatbotdb |

## Troubleshooting

### MongoDB Connection Error
```
Ensure docker-compose is running: docker-compose up -d
Check MongoDB is accessible: docker-compose logs mongodb
```

### Claude API Error
```
Verify ANTHROPIC_API_KEY is set correctly
Check API key is valid at https://console.anthropic.com
```

### Rate Limit Issues
```
Rate limits reset after 1 minute per user
Check application logs for rate limit details
```

## Sample curl Commands

```bash
# Ask a question
curl -X POST http://localhost:8081/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","sessionId":"sess1","question":"What is home loan EMI?"}'

# Get history
curl http://localhost:8081/api/chat/history/user123

# Get popular questions
curl http://localhost:8081/api/chat/popular-questions

# Health check
curl http://localhost:8081/actuator/health

# Delete history
curl -X DELETE http://localhost:8081/api/chat/history/user123
```

## Portfolio Highlights

This project demonstrates:
- ✅ Spring Boot best practices and architecture
- ✅ MongoDB integration and aggregation queries
- ✅ REST API design with proper HTTP status codes
- ✅ Rate limiting implementation
- ✅ Third-party API integration (Anthropic Claude)
- ✅ Production-ready error handling
- ✅ Domain-specific prompt engineering
- ✅ Multi-language support
- ✅ Conversation persistence and analytics

## License

This project is part of a developer portfolio.

## Contact

Built by a Java Backend Developer with 4+ years of experience in Spring Boot and banking domain.
