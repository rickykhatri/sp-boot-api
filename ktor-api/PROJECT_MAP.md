# kotlin-api — Project Map

## Stack
| Layer | Technology | Notes |
|---|---|---|
| Language | Kotlin 1.9.25 | JVM target 17 |
| Runtime | Java 17 | Spring Boot app |
| Framework | Spring Boot 3.5.0 | Web + JPA + Flyway |
| Build | Gradle Kotlin DSL | Wrapper included |
| Persistence | Spring Data JPA | Shared H2 file-based storage |
| AI / tooling | Ollama, OpenAI, local vision/image services | Configured through app properties |

---

## Repository Layout

```
kotlin-api/
├── build.gradle.kts                # Plugins, dependencies, compiler options
├── settings.gradle.kts             # Root project name
├── gradle.properties
├── gradlew / gradlew.bat           # Gradle wrapper
├── Dockerfile
├── README.md
├── HELP.md
├── PROJECT_MAP.md                  # This file
├── requests.http                   # REST Client scratchpad
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/
│   │   │   ├── KotlinApiApplication.kt
│   │   │   ├── controller/
│   │   │   │   ├── HelloController.kt
│   │   │   │   ├── UserController.kt
│   │   │   │   ├── ChatController.kt
│   │   │   │   ├── CodeController.kt
│   │   │   │   ├── ImageController.kt
│   │   │   │   ├── VisionController.kt
│   │   │   │   ├── StockController.kt
│   │   │   │   ├── ExternalApiController.kt
│   │   │   │   └── MultimodalModels.kt
│   │   │   ├── entity/
│   │   │   │   └── User.kt
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.kt
│   │   │   └── service/
│   │   │       └── UserService.kt
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/V1__create_users_table.sql
│   │       └── static/
│   │           ├── index.html
│   │           ├── index_1.html
│   │           ├── codelab.html
│   │           ├── Dashboard.html
│   │           ├── site-footer.js
│   │           ├── mic.png
│   │           └── own_logo.png
│   └── test/
│       └── kotlin/com/example/
│           └── KotlinApiApplicationTests.kt
└── build/                          # Generated output
```

---

## Application Flow

| Area | Main Files | Purpose |
|---|---|---|
| Bootstrapping | [KotlinApiApplication.kt](src/main/kotlin/com/example/KotlinApiApplication.kt) | Starts the Spring Boot application |
| Hello / sample API | [HelloController.kt](src/main/kotlin/com/example/controller/HelloController.kt) | Simple echo and demo endpoints |
| Users | [UserController.kt](src/main/kotlin/com/example/controller/UserController.kt), [UserService.kt](src/main/kotlin/com/example/service/UserService.kt), [UserRepository.kt](src/main/kotlin/com/example/repository/UserRepository.kt), [User.kt](src/main/kotlin/com/example/entity/User.kt) | CRUD for the `users` table |
| Chat | [ChatController.kt](src/main/kotlin/com/example/controller/ChatController.kt) | Chat replies via Ollama/OpenAI plus Google fallback search |
| Code runner | [CodeController.kt](src/main/kotlin/com/example/controller/CodeController.kt) | Runs safe Kotlin snippets through the scripting engine |
| Images | [ImageController.kt](src/main/kotlin/com/example/controller/ImageController.kt) | Generates images from Pollinations or a local generator |
| Vision | [VisionController.kt](src/main/kotlin/com/example/controller/VisionController.kt) | Sends uploaded images to a vision-capable Ollama model |
| Stocks | [StockController.kt](src/main/kotlin/com/example/controller/StockController.kt) | Google-based stock/quote lookup and chart helpers |
| External API | [ExternalApiController.kt](src/main/kotlin/com/example/controller/ExternalApiController.kt) | Proxies selected WM QA API calls |
| Multimodal DTOs | [MultimodalModels.kt](src/main/kotlin/com/example/controller/MultimodalModels.kt) | Shared request/response models for multimodal features |

---

## API Map

### Base REST Routes

| Method | Path | Notes |
|---|---|---|
| GET | /api/hello | Returns a hello response |
| GET | /api/hello/{name} | Personalized hello |
| POST | /api/hello | Echo request body fields |
| POST | /api/showJson | Echo arbitrary JSON |
| POST | /api/math/add | Adds two integers |
| GET | /api/ro/eta | Mock ETA payload |

### User CRUD

| Method | Path | Notes |
|---|---|---|
| GET | /api/users | List all users |
| GET | /api/users/{id} | Fetch one user |
| POST | /api/users | Create user |
| PUT | /api/users/{id} | Update user |
| DELETE | /api/users/{id} | Delete user |

### Chat

| Method | Path | Notes |
|---|---|---|
| POST | /api/chat | Main chat endpoint |
| POST | /api/send-message | Alias for chat endpoint |

### Code Runner

| Method | Path | Notes |
|---|---|---|
| POST | /api/code/run | Runs Kotlin snippet with safety checks |

### Image / Vision / Stocks

| Method | Path | Notes |
|---|---|---|
| POST | /api/image | Generates an image from a prompt |
| GET | /api/image/status | Checks image provider availability |
| POST | /api/vision | Analyzes an uploaded image |
| GET | /api/vision/status | Checks vision readiness |
| GET | /api/stocks/chart | Looks up stock results and chart info |
| GET | /api/stocks/status | Checks stock search availability |

### External API Proxy

| Method | Path | Notes |
|---|---|---|
| GET | /api/external/versions/{language} | Fetches version JSON by language |
| POST | /api/external/user | Authenticates against the upstream API |
| GET | /api/external/accounts/{userId} | Fetches authorized account data |

---

## Persistence

| File | Purpose |
|---|---|
| [application.properties](src/main/resources/application.properties) | Shared H2, chat and multimodal settings |
| [V1__create_users_table.sql](src/main/resources/db/migration/V1__create_users_table.sql) | Flyway migration for the `users` table |

The default profile uses file-based H2 storage with `spring.jpa.hibernate.ddl-auto=validate`. Set `H2_DB_PATH` to a mounted volume path in Oracle Cloud if you want data to survive restarts.

---

## Frontend Assets

The app serves a browser UI from `src/main/resources/static/`.

| File | Purpose |
|---|---|
| [index.html](src/main/resources/static/index.html) | Main chat UI |
| [codelab.html](src/main/resources/static/codelab.html) | Kotlin code lab / runner UI |
| [Dashboard.html](src/main/resources/static/Dashboard.html) | Dashboard page |
| [index_1.html](src/main/resources/static/index_1.html) | Alternate static page |
| [site-footer.js](src/main/resources/static/site-footer.js) | Footer web component/script |
| [mic.png](src/main/resources/static/mic.png) | Microphone icon |
| [own_logo.png](src/main/resources/static/own_logo.png) | Branding asset |

---

## Key Config Flags

| Setting | Default | Purpose |
|---|---|---|
| `CHAT_PROVIDER` | `ollama` | Selects Ollama or OpenAI for chat |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Local chat/vision backend |
| `OLLAMA_MODEL` | `llama3.2` | Chat model |
| `OPENAI_API_KEY` | empty | Optional OpenAI fallback |
| `VISION_OLLAMA_MODEL` | `llama3.2-vision` | Vision model |
| `IMAGE_PROVIDER` | `pollinations` | Image generation backend |
| `IMAGE_GENERATOR_URL` | `http://localhost:7860` | Local Stable Diffusion API |

---

## Notes

- The app is a single Spring Boot service with both JSON APIs and a browser-based UI.
- Chat replies are rendered in the frontend with Markdown/code formatting support.
- The stock-query route is optimized for ticker-like inputs and search-style prompts.
- `requests.http` is the quickest place to exercise the API during development.
