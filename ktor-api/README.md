# kotlin-api

A Spring Boot REST API written in Kotlin, with H2 database support, Flyway migrations, and ngrok for public tunnel exposure.

---

## Requirements

| Tool       | Version  |
|------------|----------|
| Java (JDK) | 17       |
| Gradle     | (wrapper included) |
| ngrok      | latest   |
| PostgreSQL | not required |

---

## Spring Boot Commands

### Run the app
```bash
./gradlew bootRun
```
App starts at `http://localhost:8080`

---

### Build a JAR
```bash
./gradlew build
```
Output: `build/libs/kotlin-api-0.0.1-SNAPSHOT.jar`

---

### Run the JAR directly
```bash
java -jar build/libs/kotlin-api-0.0.1-SNAPSHOT.jar
```

---

### Run tests
```bash
./gradlew test
```

### Clean build output
```bash
./gradlew clean
```

### Clean and rebuild
```bash
./gradlew clean build
```

---

## Oracle Cloud deployment

This app can be deployed to Oracle Cloud Infrastructure as a container image.

### 1) Build the deployable jar or image
The project already includes a Dockerfile, so the simplest path is:

```bash
./gradlew clean bootJar
docker build -t kotlin-api:latest .
```

### 2) Push the image to Oracle Container Registry
Tag the image for your OCI registry, then log in and push it:

```bash
docker tag kotlin-api:latest <region>.ocir.io/<tenancy>/<repo>/kotlin-api:latest
docker login <region>.ocir.io
docker push <region>.ocir.io/<tenancy>/<repo>/kotlin-api:latest
```

### 3) Create an OCI Container Instance
In Oracle Cloud, create a container instance from the pushed image and expose port `8080`.

Mount a persistent volume at `/app/data` and set these environment variables in the container configuration:

```bash
H2_DB_PATH=/app/data/kotlinapi
H2_CONSOLE_ENABLED=false
```

`H2_DB_PATH` should point at the mounted volume if you want H2 data to survive container restarts.

If you want the chat features to use OpenAI instead of local Ollama, also set:

```bash
CHAT_PROVIDER=openai
OPENAI_API_KEY=<your-openai-key>
OPENAI_MODEL=gpt-4o-mini
```

### 4) Open the public endpoint
After deployment, use the public OCI container URL on port `8080`.

---

### H2 Console (dev only)
While the app is running, open:
```
http://localhost:8080/h2-console
```
| Field            | Value                                      |
|------------------|--------------------------------------------|
| JDBC URL         | `jdbc:h2:mem:kotlinapi`                    |
| Username         | `sa`                                       |
| Password         | _(leave blank)_                            |

---

### Kill a process on port 8080
```bash
lsof -ti :8080 | xargs kill -9
```

---

## ngrok Commands

ngrok creates a secure public HTTPS tunnel to your local server — useful for testing webhooks, sharing your local API, or exposing it via a stable URL.

---

### One-time setup — save your auth token
```bash
ngrok config add-authtoken <YOUR_TOKEN>
```
Config is saved to `~/.config/ngrok/ngrok.yml`. Only needs to be run once per machine.

---

### Start a tunnel on port 8080
```bash
ngrok http 8080
```
ngrok prints a public URL like `https://xxxx-xx-xx-xxx.ngrok-free.app` that forwards to `localhost:8080`.

---

### Start a tunnel with a static/named domain (paid plan)
```bash
ngrok http --domain=your-static-domain.ngrok-free.app 8080
```

---

### Start a tunnel with a custom subdomain (paid plan)
```bash
ngrok http --subdomain=myapp 8080
```

---

### Skip the ngrok browser warning in requests
Add this header to HTTP requests going through the tunnel:
```
ngrok-skip-browser-warning: true
```

---

### Inspect live traffic (web UI)
While ngrok is running, open:
```
http://127.0.0.1:4040
```
Shows all tunneled requests and responses in real time.

---

### View ngrok config file location
```bash
ngrok config check
```

---

### List active tunnels (via ngrok API)
```bash
curl http://127.0.0.1:4040/api/tunnels
```

---

## API Endpoints

| Method | Path                | Description              |
|--------|---------------------|--------------------------|
| GET    | /api/hello          | Hello World              |
| GET    | /api/hello/{name}   | Hello by name            |
| POST   | /api/hello          | Echo name / age / email  |
| POST   | /api/showJson       | Echo any JSON payload    |
| POST   | /api/math/add       | Add two integers         |
| GET    | /api/ro/eta         | Mock RO ETA response     |
| GET    | /api/users          | Get all users            |
| GET    | /api/users/{id}     | Get user by ID           |
| POST   | /api/users          | Create user              |
| PUT    | /api/users/{id}     | Update user              |
| DELETE | /api/users/{id}     | Delete user              |

---

## Project Structure

```
src/main/kotlin/com/example/
├── KotlinApiApplication.kt     # Entry point
├── controller/
│   ├── HelloController.kt      # Hello & utility endpoints
│   └── UserController.kt       # User CRUD endpoints
├── entity/
│   └── User.kt                 # JPA entity
├── repository/
│   └── UserRepository.kt       # Spring Data JPA repository
└── service/
    └── UserService.kt          # Business logic

src/main/resources/
├── application.properties      # Shared H2, chat, and multimodal settings
└── db/migration/
    └── V1__create_users_table.sql  # Flyway migration
```
