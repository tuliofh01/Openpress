# Openpress — Plugins Overview

> **Prerequisites:** `build.gradle.kts` must include the plugin dependency, and the module must be listed in `application.yaml`

---

## 1. How Plugins Work in KTor

A plugin is installed via `install(Plugin) { /* configuration */ }` inside an `Application.()` extension function. Each plugin module is declared in `application.yaml` and loaded at startup in order.

```yaml
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

> **Order matters.** Install authentication before routes that use it.

---

## 2. Plugin Reference

### 2.1 CORS — Cross-Origin Resource Sharing

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Http.kt` |
| **Module** | `Application.configureHttp()` |
| **Dependency** | `ktor-server-cors` (already in catalog) |

**Current configuration:**
```kotlin
install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader("MyCustomHeader")
    anyHost()  // ⚠️ Allow all origins — restrict in production
}
```

**How to customize:**
```kotlin
install(CORS) {
    host("my-frontend.vercel.app")  // Restrict to specific origin
    allowCredentials = true
}
```

**Use case:** Allows frontend apps (Angular, React) hosted on different domains to call the API.

---

### 2.2 Compression — Response Compression

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Http.kt` |
| **Module** | `Application.configureHttp()` |
| **Dependency** | `ktor-server-compression` |

```kotlin
install(Compression)
```

Compresses response bodies with gzip/deflate when the client sends `Accept-Encoding`. No configuration needed — it works out of the box.

---

### 2.3 DefaultHeaders — Response Headers

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Http.kt` |
| **Module** | `Application.configureHttp()` |
| **Dependency** | `ktor-server-default-headers` |

```kotlin
install(DefaultHeaders) {
    header("X-Engine", "Ktor")  // Custom header on every response
}
```

Adds a custom `X-Engine: Ktor` header to all HTTP responses for identification.

---

### 2.4 CallId — Request Tracking

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Monitoring.kt` |
| **Module** | `Application.configureMonitoring()` |
| **Dependency** | `ktor-server-call-id` |

```kotlin
install(CallId) {
    header(HttpHeaders.XRequestId)       // Read from incoming header
    verify { callId -> callId.isNotEmpty() }  // Reject empty IDs
}
```

Every request gets a unique ID (from `X-Request-Id` header or auto-generated). This ID is available in logs for tracing requests across services.

**Access in a handler:**
```kotlin
get("/") {
    val callId = call.request.callId  // String?
}
```

---

### 2.5 ContentNegotiation — JSON Serialization

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Serialization.kt` |
| **Module** | `Application.configureSerialization()` |
| **Dependency** | `ktor-server-content-negotiation` + `ktor-serialization-kotlinx-json` |

```kotlin
install(ContentNegotiation) {
    json()  // Uses kotlinx.serialization
}
```

**What it does:**
- Automatically deserializes JSON request bodies into `@Serializable` data classes via `call.receive<T>()`
- Automatically serializes response objects to JSON via `call.respond(obj)`
- Respects `Content-Type` and `Accept` headers

**How to use:**
```kotlin
@Serializable
data class User(val name: String, val age: Int)

post("/user") {
    val user = call.receive<User>()           // JSON → User
    call.respond(HttpStatusCode.Created, user) // User → JSON
}
```

---

### 2.6 JWT Authentication

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Security.kt` |
| **Module** | `Application.configureSecurity()` |
| **Dependency** | `ktor-server-auth` + `ktor-server-auth-jwt` |

```kotlin
authentication {
    jwt {
        realm = "ktor sample app"
        verifier(
            JWT
                .require(Algorithm.HMAC256("secret"))
                .withAudience("jwt-audience")
                .withIssuer("https://jwt-provider-domain/")
                .build()
        )
        validate { credential ->
            if (credential.payload.audience.contains("jwt-audience"))
                JWTPrincipal(credential.payload)
            else null   // Reject if audience doesn't match
        }
    }
}
```

**Protecting a route:**
```kotlin
routing {
    authenticate("jwt") {
        get("/protected") {
            val principal = call.principal<JWTPrincipal>()
            call.respondText("Hello, ${principal?.payload?.subject}")
        }
    }
}
```

> **⚠️ Security note:** The JWT secret `"secret"` is hardcoded. In production, load it from an environment variable or a secrets manager.

---

### 2.7 Sessions — Cookie-Based State

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Security.kt` |
| **Module** | `Application.configureSecurity()` |
| **Dependency** | `ktor-server-sessions` |

```kotlin
install(Sessions) {
    cookie<MySession>("MY_SESSION") {
        cookie.extensions["SameSite"] = "lax"
    }
}
```

**Session model:**
```kotlin
@Serializable
data class MySession(val count: Int = 0)
```

**Using sessions in routes:**
```kotlin
get("/session/increment") {
    val session = call.sessions.get<MySession>() ?: MySession()
    call.sessions.set(session.copy(count = session.count + 1))
    call.respondText("Counter is ${session.count}")
}
```

---

### 2.8 Routing — HTTP Route Definitions

| Property | Value |
|----------|-------|
| **File** | `src/main/kotlin/Routing.kt` + `Postgres.kt` |
| **Module** | `RoutingKt.configureRouting` / `PostgresKt.configurePostgres` |
| **Dependency** | `ktor-server-core` |

```kotlin
routing {
    get("/") {
        call.respondText("Hello, World!")
    }
    staticResources("/static", "static")
    get("/json/kotlinx-serialization") {
        call.respond(mapOf("hello" to "world"))
    }
}
```

**HTTP methods supported:**
```kotlin
get("/resource")       // Read
post("/resource")      // Create
put("/resource/{id}")  // Full update
patch("/resource/{id}")// Partial update
delete("/resource/{id}")// Delete
```

**Path parameters:**
```kotlin
get("/users/{id}") {
    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
    // Use id
}
```

---

## 3. Adding a New Plugin

1. Add dependency to `build.gradle.kts`
2. Create an `Application.()` extension function:
   ```kotlin
   fun Application.configureMyPlugin() {
       install(MyPlugin) { /* config */ }
   }
   ```
3. Register the module in `application.yaml`:
   ```yaml
   - org.nexus.openpress.MyPluginKt.configureMyPlugin
   ```

---

## 4. Complete Dependency Map

```kotlin
// build.gradle.kts — all plugins and their purposes
implementation(ktorLibs.server.core)                // Routing, Application
implementation(ktorLibs.server.netty)                // Netty engine
implementation(ktorLibs.server.cors)                 // CORS
implementation(ktorLibs.server.compression)          // Response compression
implementation(ktorLibs.server.defaultHeaders)       // Default response headers
implementation(ktorLibs.server.callId)               // Request ID tracking
implementation(ktorLibs.server.contentNegotiation)   // JSON serialization
implementation(ktorLibs.serialization.kotlinx.json)  // kotlinx.json support
implementation(ktorLibs.server.auth)                 // Authentication framework
implementation(ktorLibs.server.auth.jwt)             // JWT provider
implementation(ktorLibs.server.sessions)             // Cookie sessions
implementation(ktorLibs.server.config.yaml)          // YAML config loading
implementation(ktorLibs.server.callLogging)          // Request/response logging
implementation(libs.ucasoft.ktorSimpleCache)         // Simple in-memory response caching
```
