# SmartLink - Context-Aware URL Shortener & Smart Traffic Router REST API

SmartLink is an enterprise-grade, context-aware URL shortener and smart traffic routing backend built with **Spring Boot 3** and **Java 17**. It intelligently steers incoming traffic to platform-optimized destinations (Android Play Store, iOS App Store, Web/Desktop) based on real-time device signals (`User-Agent`), while enforcing URL security, TTL expiration, click-budget circuit breakers, and granular click analytics.

---

## 🌟 Key Features

1. **Deterministic Base62 Short Code Generation**:
   - Encodes `(10000L + databaseId)` into Base62 (`0-9a-zA-Z`), guaranteeing collision-free, uniform-length short codes.
   - Supports user-defined **Custom Aliases** with uniqueness enforcement.

2. **In-Flight URL Safety & Phishing Protection (`UrlSafetyValidator`)**:
   - Enforces strict protocol validation (`http://` and `https://`).
   - Rejects blacklisted/phishing domains (`malicious-test.com`, `phishing-demo.org`, `spam-link.net`, and their subdomains) with `HTTP 400 Bad Request` (`UnsafeUrlException`).

3. **Context-Aware Dynamic Routing (`GET /r/{shortCode}`)**:
   - Inspects the caller's `User-Agent` header:
     - **Android**: Routes to `androidUrl` if configured, otherwise falls back to `defaultUrl`.
     - **iOS** (`iPhone`/`iPad`): Routes to `iosUrl` if configured, otherwise falls back to `defaultUrl`.
     - **Desktop/Other**: Routes directly to `defaultUrl`.
   - Returns an instant `HTTP 302 Found` redirect.

4. **Circuit Breaker & Fallback Handling**:
   - Enforces **Time-To-Live (TTL)** expiration (`expiresAt`).
   - Enforces **Click Cap** budgets (`maxClicks`).
   - When expired or budget-exhausted:
     - Redirects (`HTTP 302`) to `fallbackUrl` if configured.
     - Returns `HTTP 410 Gone` (`LinkExpiredException`) if no fallback is available.

5. **Granular Click Analytics (`GET /api/links/{shortCode}/stats`)**:
   - Tracks total clicks and provides device breakdown counts (`ANDROID`, `IOS`, `DESKTOP`).
   - Logs timestamp, client IP (respecting `X-Forwarded-For`), and device classification in a normalized `click_logs` table.

6. **Zero External Annotation Processors**:
   - Pure explicit Java POJOs (No Lombok, No Spring Security, No Redis) ensuring zero compilation or IDE binding friction.

---

## 🏗️ Project Architecture & Directory Layout

```
SmartLink/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── vandana/
    │   │           └── smartlink/
    │   │               ├── SmartLinkApplication.java
    │   │               ├── controller/
    │   │               │   └── LinkController.java
    │   │               ├── dto/
    │   │               │   ├── CreateLinkRequest.java
    │   │               │   ├── CreateLinkResponse.java
    │   │               │   └── LinkStatsResponse.java
    │   │               ├── exception/
    │   │               │   ├── GlobalExceptionHandler.java
    │   │               │   ├── LinkExpiredException.java
    │   │               │   ├── LinkNotFoundException.java
    │   │               │   └── UnsafeUrlException.java
    │   │               ├── model/
    │   │               │   ├── ClickLog.java
    │   │               │   └── ShortLink.java
    │   │               ├── repository/
    │   │               │   ├── ClickLogRepository.java
    │   │               │   └── ShortLinkRepository.java
    │   │               ├── service/
    │   │               │   └── LinkService.java
    │   │               └── util/
    │   │                   ├── Base62Encoder.java
    │   │                   └── UrlSafetyValidator.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/
            └── com/
                └── vandana/
                    └── smartlink/
                        ├── SmartLinkApplicationTests.java
                        ├── controller/
                        │   └── LinkControllerTest.java
                        ├── service/
                        │   └── LinkServiceTest.java
                        └── util/
                            ├── Base62EncoderTest.java
                            └── UrlSafetyValidatorTest.java
```

---

## ⚙️ Configuration (`application.properties`)

```properties
server.port=8080
spring.application.name=smartlink

# MySQL Datasource
spring.datasource.url=jdbc:mysql://localhost:3306/smartlink_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate & JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17+ installed (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- MySQL 8.0+ running on port 3306 with user `root` / password `root`

### Build Project
```bash
mvn clean test-compile
```

### Run Unit & Integration Tests
```bash
mvn test
```

### Run Spring Boot Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## 📡 REST API Documentation & Test Payloads

### 1. Create a Smart Link (Full Context Rules)
**Endpoint**: `POST /api/links`  
**Description**: Creates a short link with device routing, expiration, click budget, and fallback redirect.

#### cURL Request:
```bash
curl -X POST "http://localhost:8080/api/links" \
  -H "Content-Type: application/json" \
  -d '{
    "defaultUrl": "https://myapp.com/web",
    "customAlias": "summer2026",
    "androidUrl": "https://play.google.com/store/apps/details?id=com.myapp.android",
    "iosUrl": "https://apps.apple.com/app/id987654321",
    "fallbackUrl": "https://myapp.com/campaign-ended",
    "maxClicks": 500,
    "expiresAt": "2026-12-31T23:59:59"
  }'
```

#### Response (`201 Created`):
```json
{
  "shortCode": "summer2026",
  "shortUrl": "http://localhost:8080/r/summer2026",
  "defaultUrl": "https://myapp.com/web",
  "androidUrl": "https://play.google.com/store/apps/details?id=com.myapp.android",
  "iosUrl": "https://apps.apple.com/app/id987654321",
  "fallbackUrl": "https://myapp.com/campaign-ended",
  "maxClicks": 500,
  "expiresAt": "2026-12-31T23:59:59",
  "createdAt": "2026-11-21T16:20:00"
}
```

---

### 2. Create a Short Link with Auto Base62 Encoding
**Endpoint**: `POST /api/links`  
**Description**: Generates a collision-free Base62 short code from `10000L + databaseId`.

#### cURL Request:
```bash
curl -X POST "http://localhost:8080/api/links" \
  -H "Content-Type: application/json" \
  -d '{
    "defaultUrl": "https://github.com/spring-projects/spring-boot"
  }'
```

#### Response (`201 Created`):
```json
{
  "shortCode": "2bh",
  "shortUrl": "http://localhost:8080/r/2bh",
  "defaultUrl": "https://github.com/spring-projects/spring-boot",
  "androidUrl": null,
  "iosUrl": null,
  "fallbackUrl": null,
  "maxClicks": null,
  "expiresAt": null,
  "createdAt": "2026-11-21T16:21:00"
}
```

---

### 3. Context-Aware Traffic Redirection

#### A. Desktop Request:
```bash
curl -i -X GET "http://localhost:8080/r/summer2026" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0"
```
**Response**:
```http
HTTP/1.1 302 Found
Location: https://myapp.com/web
```

#### B. Android Request:
```bash
curl -i -X GET "http://localhost:8080/r/summer2026" \
  -H "User-Agent: Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) Mobile"
```
**Response**:
```http
HTTP/1.1 302 Found
Location: https://play.google.com/store/apps/details?id=com.myapp.android
```

#### C. iOS Request (iPhone / iPad):
```bash
curl -i -X GET "http://localhost:8080/r/summer2026" \
  -H "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X)"
```
**Response**:
```http
HTTP/1.1 302 Found
Location: https://apps.apple.com/app/id987654321
```

---

### 4. Fetch Click Analytics & Device Breakdown
**Endpoint**: `GET /api/links/{shortCode}/stats`

#### cURL Request:
```bash
curl -X GET "http://localhost:8080/api/links/summer2026/stats"
```

#### Response (`200 OK`):
```json
{
  "shortCode": "summer2026",
  "defaultUrl": "https://myapp.com/web",
  "status": "ACTIVE",
  "totalClicks": 3,
  "maxClicks": 500,
  "expiresAt": "2026-12-31T23:59:59",
  "deviceBreakdown": {
    "ANDROID": 1,
    "IOS": 1,
    "DESKTOP": 1
  }
}
```

---

### 5. Circuit Breaker & Fallback Handling

When a link reaches its `maxClicks` or its `expiresAt` passes:
- **With `fallbackUrl` configured**: The router returns `HTTP 302 Found` with `Location: <fallbackUrl>`.
- **Without `fallbackUrl` configured**: The router returns `HTTP 410 Gone`.

```bash
curl -i -X GET "http://localhost:8080/r/expired-link"
```
**Response (`410 Gone`)**:
```json
{
  "timestamp": "2026-11-21T16:22:15",
  "status": 410,
  "error": "Gone",
  "message": "Short link 'expired-link' has expired or reached its maximum click limit.",
  "path": "/r/expired-link"
}
```

---

### 6. URL Safety Filter & Malicious Domain Defense

Attempting to register a blacklisted or phishing domain triggers `UnsafeUrlException`:

```bash
curl -X POST "http://localhost:8080/api/links" \
  -H "Content-Type: application/json" \
  -d '{
    "defaultUrl": "http://malicious-test.com/payload"
  }'
```

**Response (`400 Bad Request`)**:
```json
{
  "timestamp": "2026-11-21T16:22:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Blocked malicious or phishing domain: malicious-test.com",
  "path": "/api/links"
}
```
