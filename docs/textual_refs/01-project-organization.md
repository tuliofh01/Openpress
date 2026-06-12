# Openpress — Project Organization Guide

> **Based on KTor best practices and MVC architecture**
> **Prerequisites:** Familiarity with Kotlin, Gradle, and basic KTor concepts
> **Status:** 🚧 Partially implemented — The directory structure and layered architecture described below is the **target design**. Currently the codebase has a flat `src/main/kotlin/` layout with demo CRUD (City) and a User model. The `services/`, `routes/`, and `controllers/` subdirectories are planned for v0.2.

---

## 1. Directory Structure

```
openpress/
├── build.gradle.kts              # Plugins & dependencies
├── settings.gradle.kts           # Project name, version catalogs
├── gradle.properties             # Kotlin code style config
├── gradle/
│   ├── libs.versions.toml        # Version catalog (non-Ktor deps)
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── models/            # @Serializable data classes
│   │   │   ├── services/          # Business logic (DAO / service layer)
│   │   │   └── *.kt               # Top-level: plugins, routes, entry point
│   │   ├── resources/
│   │   │   ├── application.yaml   # Ktor module config
│   │   │   ├── logback.xml        # Logging config
│   │   │   └── static/            # Static frontend files
│   │   └── ...
│   └── test/
│       └── kotlin/               # Unit & integration tests
├── docs/
│   ├── system_diagrams/          # Architecture diagrams (SVG)
│   │   └── sketches/             # PlantUML source files (.puml)
│   └── textual_refs/             # Markdown documentation
└── assets/                       # Unused — placeholder
```

---

## 2. Core Components

### 2.1 Models (`models/`)

Data classes annotated with `@Serializable` for automatic JSON serialization.

```kotlin
// src/main/kotlin/models/User.kt
@Serializable
data class User(
    val id: Int,
    val email: String,
    val password: String,
    val authStatus: Boolean,
    val role: UserRoles
)

enum class UserRoles { ADMIN, AUTHOR, CLERK, USER }
```

**Conventions:**
- Every model gets its own file under `models/`
- Use `@Serializable` from `kotlinx.serialization`
- Keep models pure — no business logic, only data

### 2.2 Services (`services/`)

Business logic and data access layer. Should be `suspend` functions for non-blocking I/O.

```kotlin
// src/main/kotlin/services/UserService.kt
class UserService(private val db: Connection) {

    suspend fun getUser(id: Int): User = withContext(Dispatchers.IO) {
        // SQL query → map to User
    }

    suspend fun createUser(user: User): Int = withContext(Dispatchers.IO) {
        // INSERT → return generated ID
    }
}
```

**Conventions:**
- One class per aggregate root
- All public functions are `suspend`
- Database operations run on `Dispatchers.IO`
- Accept a `Connection` via constructor injection

### 2.3 Controllers / Routes (`*.kt`)

HTTP request handlers defined inside `routing { }` blocks. These act as the controller layer.

```kotlin
// Routes can be organized across multiple files
fun Application.configureUserRoutes() {
    routing {
        get("/users") {
            val users = userService.getAll()
            call.respond(HttpStatusCode.OK, users)
        }
        post("/users") {
            val user = call.receive<User>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }
    }
}
```

**Conventions:**
- Group related routes in dedicated files (e.g. `UserRoutes.kt`, `OrderRoutes.kt`)
- Each group registered as a separate module in `application.yaml`
- Keep handlers thin — delegate logic to services

### 2.4 Plugins (Configuration)

Plugins are registered in `application.yaml` as modules and configured in top-level `*.kt` files.

```yaml
# application.yaml — module load order matters
ktor:
  application:
    modules:
      - org.nexus.openpress.HttpKt.configureHttp
      - org.nexus.openpress.MonitoringKt.configureMonitoring
      - org.nexus.openpress.SerializationKt.configureSerialization
      - org.nexus.openpress.SecurityKt.configureSecurity
      - org.nexus.openpress.PostgresKt.configurePostgres
      - org.nexus.openpress.RoutingKt.configureRouting
```

Each module is an `Application.()` extension function:

```kotlin
fun Application.configureSerialization() {
    install(ContentNegotiation) { json() }
}
```

---

## 3. Dependency Management

### 3.1 Version Catalogs

Ktor versions come from the official catalog (`ktorLibs`):

```kotlin
// settings.gradle.kts
versionCatalogs {
    create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.0")
}
```

Non-Ktor dependencies are in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.21"
postgresql = "42.7.10"

[libraries]
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgresql" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

### 3.2 Adding a New Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation(ktorLibs.server.core)       // Ktor catalog
    implementation(libs.postgresql)             // Local catalog
    implementation("com.example:my-lib:1.0")   // Direct coordinate
}
```

---

## 4. Request Lifecycle

```
Client Request
    │
    ▼
Netty Engine (port 8080)
    │
    ▼
Plugin Chain (in order):
  ┌─ CORS                  ─ check allowed origins/methods
  ├─ Compression            ─ decompress request body
  ├─ DefaultHeaders         ─ add X-Engine: Ktor
  ├─ CallId                 ─ assign X-Request-Id
  ├─ ContentNegotiation     ─ deserialize JSON body
  ├─ JWT Authentication     ─ verify Bearer token
  └─ Sessions               ─ resolve session cookie
    │
    ▼
Route Matching → Handler → Service → Database
    │
    ▼
Response (auto-serialized to JSON via ContentNegotiation)
```

---

## 5. Adding a New Feature

1. **Model** — Create `src/main/kotlin/models/YourModel.kt` with `@Serializable` data class
2. **Service** — Create `src/main/kotlin/services/YourService.kt` with DAO operations
3. **Routes** — Add a `configureYourRoutes()` function in a new file
4. **Register** — Add the module to `application.yaml`
5. **Test** — Add tests in `src/test/kotlin/`

---

## 6. Diagram Reference

### Structural Diagrams
![Database Entities (ERD)](../system_diagrams/(ERD)%20openpress-database-entities.svg)
![UML Class Diagram](../system_diagrams/(UML)%20openpress-class-diagram.svg)
![MVC Architecture](../system_diagrams/(UML)%20openpress-mvc-architecture.svg)
![Request Handling Flow](../system_diagrams/(KTor)%20request-handling-flow.svg)

### Use Case Diagrams
![Use Case Overview](../system_diagrams/(UML)%20openpress-usecase-overview.svg)
![Admin Use Case Detail](../system_diagrams/(UML)%20openpress-usecase-admin.svg)

### Sequence Diagrams
![Login Flow](../system_diagrams/(UML)%20openpress-sequence-login.svg)
![Checkout Flow](../system_diagrams/(UML)%20openpress-sequence-checkout.svg)
![Blog Publish Flow](../system_diagrams/(UML)%20openpress-sequence-blog-publish.svg)
![Admin Theme Flow](../system_diagrams/(UML)%20openpress-sequence-admin-theme.svg)

> **PUML sources** are available under [`docs/system_diagrams/sketches/`](../system_diagrams/sketches/) and can be re-rendered with PlantUML.
