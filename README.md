# 🔥 Habit Tracker — Java Spring Boot

A habit tracker with streaks, progress stats, and a full REST API.

---

## ✨ Features

| Feature | Details |

| **Track Daily Habits** | Create habits with name, emoji, frequency, and category |
| **Streak System** | Auto-tracks consecutive completions, resets on missed days |
| **Longest Streak** | Records your personal best for every habit |
| **Milestone Badges** | 🔥 7, 14, 21, 30, 60, 100, 365-day celebrations |
| **Mood Logging** | Log mood (GREAT/GOOD/NEUTRAL/BAD/TERRIBLE) per completion |
| **Progress Stats** | Completion rates, last 7/30 day summaries, calendar data |
| **Dashboard** | Global stats, today's habits, top streaks, 30-day graph |
| **H2 + JPA** | In-memory DB for dev; swap to PostgreSQL/MySQL for prod |
| **REST API** | Full CRUD + completion + stats endpoints |
| **Global Error Handling** | Structured `ApiResponse<T>` for all responses |
| **Scheduled Task** | Midnight cron evaluates streak breaks automatically |
| **Demo Data** | 5 sample habits seeded on startup |
| **Unit Tests** | Mockito-based service tests |
| **Integration Tests** | MockMvc full API tests |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Run the App

```bash
cd habit-tracker
mvn spring-boot:run
```

Server starts at: **http://localhost:8080**

H2 Console: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:habitdb`
- Username: `sa` / Password: *(empty)*

### Run Tests

```bash
mvn test
```

---

## 📁 Project Structure

```
src/
├── main/java/com/habittracker/
│   ├── HabitTrackerApplication.java      # Entry point
│   ├── model/
│   │   ├── Habit.java                    # JPA entity + domain logic
│   │   ├── HabitLog.java                 # Completion log entry
│   │   ├── HabitFrequency.java           # DAILY / WEEKLY / WEEKDAYS / WEEKENDS
│   │   ├── HabitCategory.java            # HEALTH / FITNESS / LEARNING / ...
│   │   └── MoodRating.java               # GREAT / GOOD / NEUTRAL / BAD / TERRIBLE
│   ├── repository/
│   │   ├── HabitRepository.java          # Spring Data JPA queries
│   │   └── HabitLogRepository.java
│   ├── service/
│   │   └── HabitService.java             # Business logic + streak engine
│   ├── controller/
│   │   ├── HabitController.java          # /api/habits REST endpoints
│   │   ├── DashboardController.java      # /api/dashboard endpoints
│   │   └── GlobalExceptionHandler.java   # Unified error responses
│   └── util/
│       ├── HabitDto.java                 # All request/response DTOs
│       ├── HabitMapper.java              # Entity → DTO mapping
│       └── DataInitializer.java          # Demo seed data
├── resources/
│   └── application.properties
└── test/
    ├── HabitServiceTest.java             # Unit tests (Mockito)
    ├── HabitControllerIntegrationTest.java # Integration tests (MockMvc)
    └── resources/application-test.properties
```

---

## 🌐 REST API Reference

### Habits

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/habits` | List all habits (`?activeOnly=true`) |
| `POST` | `/api/habits` | Create a new habit |
| `GET` | `/api/habits/{id}` | Get a habit by ID |
| `PUT` | `/api/habits/{id}` | Update a habit |
| `DELETE` | `/api/habits/{id}` | Delete a habit |
| `POST` | `/api/habits/{id}/complete` | Mark habit as done |
| `DELETE` | `/api/habits/{id}/complete` | Undo completion |
| `GET` | `/api/habits/{id}/logs` | Get all completion logs |
| `GET` | `/api/habits/{id}/stats` | Get detailed stats |

### Dashboard

| Method | Endpoint | Description |

| `GET` | `/api/dashboard` | Full dashboard stats |
| `GET` | `/api/dashboard/milestones` | Active streak milestones |

---

## 📋 Request / Response Examples

### Create Habit
```json
POST /api/habits
{
  "name": "Morning Run",
  "description": "5km every morning",
  "frequency": "DAILY",
  "category": "FITNESS",
  "emoji": "🏃"
}
```

### Complete a Habit
```json
POST /api/habits/1/complete
{
  "mood": "GREAT",
  "note": "Felt amazing today!"
}
```

### Response Format
```json
{
  "success": true,
  "message": "🎉 Habit completed! Keep the streak alive!",
  "data": {
    "id": 1,
    "name": "Morning Run",
    "currentStreak": 7,
    "longestStreak": 7,
    "completedToday": true,
    "completionRate": 85.7
  }
}
```

---

## 🏭 Production Upgrade Path

### Switch to PostgreSQL

Replace H2 in `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/habitdb
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### Add Spring Security (JWT)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Add Multi-User Support
- Add a `User` entity
- Add `@ManyToOne User owner` to `Habit`
- Filter all queries by authenticated user

---

## 🔧 Streak Engine Logic

```
Complete today:
  If already completed today → no-op
  Else if last completed = yesterday → streak++
  Else → streak = 1 (restart)
  Update longestStreak if needed

Daily midnight cron:
  For each active habit:
    If lastCompleted < yesterday → currentStreak = 0
```

---

## 📦 Tech Stack

- **Java 17**
- **Spring Boot 3.2**
- **Spring Data JPA / Hibernate**
- **H2** (dev) / PostgreSQL-ready (prod)
- **Lombok**
- **JUnit 5 + Mockito**
- **MockMvc** for integration testing
