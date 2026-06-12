# Openpress — JWT Route Security

> **Prerequisites:**
> - JWT authentication configured (see [`02-plugins-overview.md`](./02-plugins-overview.md#26-jwt-authentication))
> - User model with `role` field defined in `src/main/kotlin/models/User.kt`
> - Session management active
> **Status:** 🚧 Planned — This document describes the **target JWT security architecture** for the project. Currently no routes are JWT-protected; the existing routes (root `/`, City CRUD, session counter) are all public. The `UserService`, `OrderService`, `UserRoutes`, `OrderRoutes`, `AuthRoutes`, and `AdminRoutes` classes shown below have not been implemented yet.

---

## 1. The User Model

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

The `role` field is the basis for all authorization decisions.

---

## 2. Protecting Routes with `authenticate {}`

KTor's `authenticate { }` block wraps routes so they require a valid JWT token.

```kotlin
routing {
    authenticate("jwt") {
        // All routes inside here require a valid JWT Bearer token
        get("/users") {
            // Only reachable if JWT is valid
            call.respond(HttpStatusCode.OK, "Authenticated")
        }
    }

    // Routes outside are public
    get("/") {
        call.respondText("Hello, World!")
    }
}
```

---

## 3. Extracting Roles from JWT Claims

To perform role-based access control, you need to store the user's role inside the JWT token claims.

### 3.1 Include Role in Token Creation

When issuing a JWT token, include the role as a custom claim:

```kotlin
fun createToken(user: User): String {
    return JWT.create()
        .withSubject(user.id.toString())
        .withClaim("role", user.role.name)  // Custom claim
        .withIssuer("https://your-domain.com")
        .withAudience("openpress-api")
        .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))  // 1 hour
        .sign(Algorithm.HMAC256("secret"))
}
```

### 3.2 Extract Role on the Server

```kotlin
fun PipelineContext<Unit, ApplicationCall>.currentUserRole(): UserRoles? {
    val principal = call.principal<JWTPrincipal>()
    val roleName = principal?.payload?.getClaim("role")?.asString()
    return roleName?.let { UserRoles.valueOf(it) }
}
```

---

## 4. Role-Based Access Control (RBAC) in Routes

Combine `authenticate { }` with a role check helper:

```kotlin
routing {
    authenticate("jwt") {
        // ── ADMIN only ──────────────────────────────────
        get("/users") {
            if (currentUserRole() != UserRoles.ADMIN) {
                call.respond(HttpStatusCode.Forbidden, "Admin access required")
                return@get
            }
            val users = userService.getAll()
            call.respond(HttpStatusCode.OK, users)
        }

        post("/users") {
            if (currentUserRole() != UserRoles.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }
            val newUser = call.receive<User>()
            val id = userService.create(newUser)
            call.respond(HttpStatusCode.Created, id)
        }

        put("/users/{id}") {
            if (currentUserRole() != UserRoles.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@put
            }
            val id = call.parameters["id"]?.toInt() ?: 0
            val updatedUser = call.receive<User>()
            userService.update(id, updatedUser)
            call.respond(HttpStatusCode.OK)
        }

        delete("/users/{id}") {
            if (currentUserRole() != UserRoles.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@delete
            }
            val id = call.parameters["id"]?.toInt() ?: 0
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }

        // ── Multi-role access ────────────────────────────
        get("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: 0
            val currentRole = currentUserRole()

            // ADMIN and AUTHOR can view any user
            if (currentRole == UserRoles.ADMIN || currentRole == UserRoles.AUTHOR) {
                val user = userService.getById(id)
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
```

---

## 5. Recommended Folder Structure for Routes

As the project grows, split routes into dedicated files:

```
src/main/kotlin/
├── routes/
│   ├── UserRoutes.kt       # /users — all CRUD + role checks
│   ├── AuthRoutes.kt       # /auth/login, /auth/register
│   ├── OrderRoutes.kt      # /orders — customer & admin routes
│   └── AdminRoutes.kt      # /admin — ADMIN-only operations
├── services/
│   ├── UserService.kt
│   └── OrderService.kt
└── models/
    ├── User.kt
    └── Order.kt
```

Each routes file exports a module:

```kotlin
// routes/UserRoutes.kt
fun Application.configureUserRoutes() {
    routing {
        authenticate("jwt") {
            get("/users") { /* ... */ }
            post("/users") { /* ... */ }
        }
    }
}
```

Registered in `application.yaml`:

```yaml
ktor:
  application:
    modules:
      - org.nexus.openpress.UserRoutesKt.configureUserRoutes
      - org.nexus.openpress.AuthRoutesKt.configureAuthRoutes
```

---

## 6. Complete Flow: Request Lifecycle

```
1. Client sends: GET /users
   Header: Authorization: Bearer <token>

2. Netty receives request

3. JWT plugin:
   ├─ Verifies token signature (HMAC256)
   ├─ Validates audience & issuer claims
   ├─ Checks expiration
   └─ If valid → sets JWTPrincipal on call

4. Route matched: get("/users")

5. Handler:
   ├─ currentUserRole() extracts "role" claim
   ├─ Compares against UserRoles.ADMIN
   └─ Forbidden (403) or OK (200) with user list
```

---

## 7. Security Best Practices

| Practice | Implementation |
|----------|---------------|
| Use environment variables for secrets | `System.getenv("JWT_SECRET")` instead of hardcoded `"secret"` |
| Short token expiration | 15–60 minutes, implement refresh tokens |
| Validate all inputs | Never trust `call.parameters` or `call.receive<T>()` directly |
| Return generic errors | Avoid leaking whether a user exists or not |
| Log auth failures | Use `callLogging` plugin or SLF4j directly |

---

## 8. Diagram Reference

![Request Handling](../system_diagrams/(KTor)%20request-handling-flow.svg)
