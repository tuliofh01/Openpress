# Openpress — Self-Hostable Blog & E-Commerce Platform

> **by Túlio F. Horta** · Powered by Kotlin (KTor), Angular, PostgreSQL
> A free, self-hosted alternative to WordPress, Wix, and Blogspot for small and medium businesses, authors, and creators.

---

## The Idea

### The Problem

Proprietary website platforms come with a growing list of frustrations:

- **Recurring costs** — Monthly fees for hosting, plugins, themes, and "premium" features that should be standard
- **Vendor lock-in** — Your content, customers, and data live on someone else's servers. Switching platforms means starting from scratch
- **Limited control** — Free tiers insert ads, restrict customization, and limit your audience reach
- **Complex licensing** — WordPress is open-source but requires plugin licenses, premium themes, and constant security maintenance
- **Feature fragmentation** — Need a blog? A store? A membership area? That's three different services with three different bills

### The Solve

Openpress is a **single, self-hosted platform** that combines everything a modern online presence needs:

- A **blog engine** with categories, tags, and threaded comments
- An **online store** with product variants, stock tracking, and Stripe payments
- A **shopping cart and checkout** system with order fulfillment
- **Role-based staff management** — Admin, Author, Clerk, User — each with granular permissions
- An **admin dashboard** for managing products, posts, users, orders, and site configuration
- **Theme customization** — Change colors, fonts, and layout through the admin panel without touching code

All in one codebase. No monthly fees. No feature gates. Your data, your infrastructure.

### Who It's For

| Audience | Use Case |
|----------|----------|
| **Small businesses** | Online store + blog + customer management under one roof |
| **Authors & journalists** | Publish articles, build an audience, sell books or merch |
| **Influencers & creators** | Content hub + merch store + newsletter/community features |
| **Freelancers & agencies** | Deploy for clients as a white-label solution, no per-client licensing |
| **Developers** | Full Kotlin + Angular codebase to customize, extend, and contribute to |

### Philosophy

> **Simple to deploy, simple to run, simple to customize.**

Openpress prioritizes:
- **Developer experience** — Kotlin's conciseness, KTor's lightweight async model, Gradle's build system
- **Operational simplicity** — One JAR, one database, one server. No microservices, no container orchestration needed (but Docker is supported)
- **Data ownership** — You run it, you own it. Export your data anytime
- **No dark patterns** — No upsells, no data mining, no forced ads

---

## Architecture Overview

### Business Domains

The platform is organized into 7 business domains, each with dedicated entities, services, and routes:

| Domain | Entities | Description |
|--------|----------|-------------|
| **Auth** | User, UserRoles, Session | Authentication, JWT token management, role-based access control |
| **Content** | Post, Category, Tag, Comment | Blog engine with categories, tags, and threaded comments |
| **Catalog** | Product, ProductVariant, Category | Product catalog with variants (size, color), nested categories |
| **Cart** | Cart, CartItem | Authenticated shopping cart |
| **Orders** | Order, OrderItem, Shipment | Order placement, fulfillment tracking |
| **Inventory** | StockMovement, StockEntry | Stock tracking and movement logging |
| **Admin** | ThemeConfig, SystemLog, SiteSettings | Admin dashboard configuration, system monitoring |

### Layered Architecture (MVC-Inspired)

The backend follows a strict layered pattern: **Controller → Service → Repository → Model**.

```
Browser / Mobile App
       │
       ▼
 ┌─────────────────────────────────────────┐
 │         KTor Plugin Pipeline            │
 │  CORS → Compression → DefaultHeaders   │
 │  → CallId → ContentNegotiation → JWT   │
 │  → Sessions → Routing                  │
 └─────────────────────────────────────────┘
       │
       ▼
 ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 │Controller │───▶│ Service  │───▶│Repository│───▶│  Model   │
 │(routes)   │    │(business)│    │(JDBC/SQL)│    │(entities)│
 └──────────┘    └──────────┘    └──────────┘    └──────────┘
       │                                              │
       ▼                                              ▼
  JSON Response                                    Database
  (via ContentNegotiation)                    (H2 / PostgreSQL)
```

**Layer responsibilities:**

1. **Controller** (route handlers) — Receive HTTP requests, deserialize input, delegate to service, serialize response
2. **Service** (business logic) — Orchestrate operations, enforce rules, coordinate between models
3. **Repository** (data access) — SQL queries via JDBC, CRUD operations, connection management
4. **Model** (entities) — `@Serializable` data classes representing database tables

### Plugin Pipeline

The KTor server processes every request through a configured chain of plugins. Order matters:

| # | Plugin | Responsibility | Source File |
|---|--------|---------------|-------------|
| 1 | **CORS** | Allow cross-origin requests from frontend | `Http.kt` |
| 2 | **Compression** | Decompress request bodies (gzip/deflate) | `Http.kt` |
| 3 | **DefaultHeaders** | Add `X-Engine: Ktor` header to all responses | `Http.kt` |
| 4 | **CallId** | Assign unique `X-Request-Id` for request tracing | `Monitoring.kt` |
| 5 | **ContentNegotiation** | Deserialize JSON → `@Serializable` models and serialize responses | `Serialization.kt` |
| 6 | **JWT Auth** | Verify Bearer tokens, extract principal and role claims | `Security.kt` |
| 7 | **Sessions** | Resolve cookie-based session state | `Security.kt` |

### API Contract

The Angular frontend communicates with the backend via a **RESTful JSON API**:

- All request/response bodies use `Content-Type: application/json`
- Authentication via `Authorization: Bearer <jwt>` header
- Role-based endpoints grouped by domain (e.g., `/admin/*`, `/orders/*`, `/posts/*`)
- Standard HTTP status codes: 200, 201, 400, 401, 403, 404, 500

See the [Angular Frontend Integration guide](./docs/textual_refs/09-angular-frontend.md) for the full API contract.

---

## System Diagrams

The following diagrams (generated from PlantUML sources in `docs/system_diagrams/sketches/`) describe the platform's architecture, data model, interactions, and usage scenarios:

| Diagram | What it shows |
|---------|---------------|
| **Request Handling Flow** | The lifecycle of an HTTP request: browser → Netty → plugin pipeline (CORS, Compression, Headers, CallId, ContentNegotiation, JWT, Sessions) → router → handler → service → database → response |
| **MVC Architecture** | UML deployment view (nodes: Browser, KTor Server, Database) + component view (Router, Controller, Service, Model, View) with an example POST /cities lifecycle |
| **Database Entities (ERD)** | All 27 database tables across 7 domains (Auth, Content, Catalog, Cart, Orders, Inventory, Admin) with crow's-foot relationships, primary keys, foreign keys, and column types |
| **UML Class Diagram** | Complete class model: 27 data classes, 10 repository interfaces + JDBC implementations, 8 service classes, 8 controller groups, 7 plugin configurations, 3 enums, and cross-layer dependencies |
| **Use Case Overview** | All 5 actor roles (Visitor, User, Author, Clerk, Admin) mapped to their use cases — browsing, shopping, content management, stock control, and system administration |
| **Admin Use Case Detail** | Granular view of the Admin role across 6 domains: User Management, Content, Shop, Orders, System Configuration, and Monitoring |
| **Login Sequence** | Login flow: user submits credentials → AuthService verifies via UserRepository → JWT issued → browser stores token → redirect to dashboard (with 401 error path) |
| **Checkout Sequence** | Cart-to-order flow: add items → place order → reserve stock → process payment → create shipment (with payment failure path) |
| **Blog Publish Sequence** | Post creation and publishing: author creates draft → adds categories/tags → publishes → slug generation → 201 response |
| **Admin Theme Sequence** | Theme customization flow: admin sets colors/title via AdminPanel → SettingRepository persists → Angular fetches GET /api/theme-config → applies CSS custom properties dynamically |

---

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| **Language** | Kotlin 2.x | Concise, null-safe, coroutine-native, first-class JVM language |
| **Backend Framework** | KTor 3.x (Netty engine, port 8080) | Lightweight, async-first, Kotlin-native DSL for routing and plugins |
| **Serialization** | kotlinx.serialization (JSON) | Compile-time safe, no reflection, Kotlin-native |
| **Authentication** | JWT (HMAC256) + Cookie Sessions | Stateless auth for API, stateful sessions for admin panel |
| **Database** | PostgreSQL (production) / H2 embedded (development) | H2 for zero-setup dev, PostgreSQL for production reliability |
| **Database Access** | JDBC + raw SQL | Direct control over queries, no ORM overhead |
| **Frontend** | Angular (separate project or monorepo proxy) | Component-based, TypeScript-native, strong CLI ecosystem |
| **Payments** | Stripe SDK | Industry-standard payment processing, PCI-compliant via Stripe Elements |
| **Infrastructure** | Docker · Docker Compose · Jenkins · Nginx | Containerized deployment, CI/CD pipeline, reverse proxy with HTTPS |

---

## Documentation Map

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 01 | [Project Organization](./docs/textual_refs/01-project-organization.md) | Directory structure, naming conventions, model/service/controller patterns | ✅ Basic |
| 02 | [Plugins Overview](./docs/textual_refs/02-plugins-overview.md) | All 7 KTor plugins: CORS, Compression, DefaultHeaders, CallId, ContentNegotiation, JWT, Sessions | ✅ Complete |
| 03 | [JWT Route Security](./docs/textual_refs/03-jwt-route-security.md) | Securing routes with JWT, role extraction, RBAC | 📝 Planned |
| 04 | [PostgreSQL Setup](./docs/textual_refs/04-postgresql-setup.md) | Install & configure PostgreSQL (Arch, Debian, CentOS, Windows) | ✅ Complete |
| 05 | [Stripe Integration](./docs/textual_refs/05-stripe-integration.md) | Payment processing with Stripe — SDK, webhooks, security | 📝 Planned |
| 06 | [Docker Integration](./docs/textual_refs/06-docker-integration.md) | Dockerfile, docker-compose (app + PostgreSQL), build & run | 📝 Planned |
| 07 | [Jenkins CI/CD](./docs/textual_refs/07-jenkins-integration.md) | Jenkins pipeline — compile, test, build, deploy | 📝 Planned |
| 08 | [Expose API to Web](./docs/textual_refs/08-expose-api-to-web.md) | Port forwarding, ngrok, Nginx reverse proxy, domain + HTTPS | ✅ Complete |
| 09 | [Angular Frontend](./docs/textual_refs/09-angular-frontend.md) | Angular setup, Vercel deployment, monorepo proxy, theme integration | 📝 Planned |

---

## Quick Start

**Requirements:** JDK 21, PostgreSQL (optional — H2 works by default in dev mode)

```bash
# Clone and build
git clone https://github.com/your-org/openpress
cd openpress
./gradlew build

# Run (starts at http://localhost:8080)
./gradlew run
```

The server starts with an embedded H2 database in development mode. No PostgreSQL installation needed.

**Useful Gradle commands:**

| Command | Description |
|---------|-------------|
| `./gradlew run` | Start the KTor development server |
| `./gradlew build` | Compile, test, and package |
| `./gradlew test` | Run unit and integration tests |
| `./gradlew buildFatJar` | Build a standalone executable JAR |

For a full production deployment with PostgreSQL and Docker, see the [Docker integration guide](./docs/textual_refs/06-docker-integration.md) and the [API exposure guide](./docs/textual_refs/08-expose-api-to-web.md).

---

## Project Roadmap

| Phase | Focus | Status |
|-------|-------|--------|
| **v0.1** | KTor server foundation: plugin pipeline, basic routing, City CRUD demo, User model | 🟢 Current |
| **v0.2** | Full entity layer (27 models), repository interfaces + JDBC implementations, service classes, controller routes, JWT-secured endpoints | 🔜 Next |
| **v0.3** | Angular frontend: product browsing, cart, checkout, auth, admin dashboard, theme system | 📝 Planned |
| **v1.0** | Dockerized production deployment, Stripe payments, Jenkins CI/CD pipeline, Nginx reverse proxy with HTTPS | 📝 Planned |

---

## Project Structure

```
openpress/
├── build.gradle.kts              # Plugins & dependencies
├── settings.gradle.kts           # Project name, KTor version catalog
├── gradle.properties             # Kotlin code style config
├── gradle/
│   ├── libs.versions.toml        # Version catalog (non-KTor deps)
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── kotlin/               # Application source (flat layout, v0.1)
│   │   │   ├── main.kt           # Entry point
│   │   │   ├── Http.kt           # CORS, Compression, DefaultHeaders
│   │   │   ├── Monitoring.kt     # CallId plugin
│   │   │   ├── Serialization.kt  # ContentNegotiation (JSON)
│   │   │   ├── Security.kt       # JWT Auth + Sessions
│   │   │   ├── Routing.kt        # General routes
│   │   │   ├── Postgres.kt       # DB connection + City CRUD routes
│   │   │   ├── CitySchema.kt     # City model + CityService (demo)
│   │   │   ├── MySession.kt      # Session data class
│   │   │   └── models/
│   │   │       └── User.kt       # User model + UserRoles enum
│   │   └── resources/
│   │       ├── application.yaml  # KTor module config
│   │       └── logback.xml       # Logging config
│   └── test/
│       └── kotlin/               # Unit & integration tests
├── docs/
│   ├── system_diagrams/          # Architecture diagrams (SVG)
│   │   └── sketches/             # PlantUML source files (.puml)
│   └── textual_refs/             # Markdown documentation
└── assets/                       # Placeholder
```

---

## Project Status

**Phase:** Early development (v0.1)

The backend foundation exists — KTor server with 7 plugins configured, basic routing (Hello World, JSON endpoint, session counter), and a City CRUD demo. The User model is defined. The full data model (27 entities across 7 domains), repository interfaces, service classes, controller routes, and Angular frontend are designed and documented in the diagrams but not yet implemented in code.

Start with the [Documentation Index](./docs/textual_refs/index.md) for a guided tour.
