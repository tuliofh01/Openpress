# Openpress — Documentation Index

> **Project:** Openpress v0.1 — KTor-based e-commerce & blogging platform
> **Root:** [`project root`](..)
> **Status:** Early development — backend foundation exists; full entity model, services, frontend, and infrastructure are designed but not yet implemented.

Openpress is a self-hostable, all-in-one web platform for small and medium businesses that combines a **blog engine**, an **online store**, role-based **staff management**, and an **admin dashboard** — all running on a Kotlin/KTor backend with an Angular frontend. It is designed as a free, open-source alternative to proprietary platforms such as WordPress, Wix, and Blogspot.

The backend is organized around an MVC-inspired layered architecture (Controller → Service → Repository → Model), secured via JWT authentication and a configurable KTor plugin pipeline (CORS, Compression, DefaultHeaders, CallId, ContentNegotiation, Sessions). The full data model spans 27 entities across 7 domains (Auth, Content, Catalog, Cart, Orders, Inventory, Admin), and the Angular frontend communicates with the API through a RESTful JSON contract.

---

## Where to Start

Depending on your role, here's the recommended reading path:

| You are... | Start with... |
|------------|---------------|
| **Backend developer** | [01 — Project Organization](./01-project-organization.md) → [02 — Plugins Overview](./02-plugins-overview.md) → [03 — JWT Route Security](./03-jwt-route-security.md) |
| **Frontend developer** | [09 — Angular Frontend](./09-angular-frontend.md) + [02 — Plugins (CORS section)](./02-plugins-overview.md#21-cors) |
| **DevOps / Infrastructure** | [06 — Docker Integration](./06-docker-integration.md) → [07 — Jenkins CI/CD](./07-jenkins-integration.md) → [08 — Expose API to Web](./08-expose-api-to-web.md) |
| **Database admin** | [04 — PostgreSQL Setup](./04-postgresql-setup.md) + [ERD Diagram](../system_diagrams/(ERD)%20openpress-database-entities.svg) |
| **New to the project** | Start here (index) → skim the [System Diagrams](#system-diagrams) → [01 — Project Organization](./01-project-organization.md) |
| **Implementing payments** | [05 — Stripe Integration](./05-stripe-integration.md) + [Checkout Sequence Diagram](../system_diagrams/(UML)%20openpress-sequence-checkout.svg) |

---

## Project Architecture

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 01 | [`01-project-organization.md`](./01-project-organization.md) | Directory structure, models, controllers, services, naming conventions | ✅ Basic |
| 02 | [`02-plugins-overview.md`](./02-plugins-overview.md) | Every KTor plugin configured — CORS, Compression, DefaultHeaders, CallId, ContentNegotiation, JWT Auth, Sessions, Routing | ✅ Complete |
| 03 | [`03-jwt-route-security.md`](./03-jwt-route-security.md) | Securing routes with JWT, extracting roles, `authenticate {}` block, RBAC | 📝 Planned |

## Database & Payments

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 04 | [`04-postgresql-setup.md`](./04-postgresql-setup.md) | Install & configure PostgreSQL on Arch, Debian, CentOS, Windows | ✅ Complete |
| 05 | [`05-stripe-integration.md`](./05-stripe-integration.md) | Payment processing with Stripe — SDK, webhooks, security | 📝 Planned |

## DevOps & Deployment

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 06 | [`06-docker-integration.md`](./06-docker-integration.md) | Dockerfile, docker-compose (app + PostgreSQL), build & run | 📝 Planned |
| 07 | [`07-jenkins-integration.md`](./07-jenkins-integration.md) | Jenkins CI/CD pipeline — compile, test, build, deploy | 📝 Planned |
| 08 | [`08-expose-api-to-web.md`](./08-expose-api-to-web.md) | Port forwarding, ngrok, Nginx reverse proxy, domain + HTTPS | ✅ Complete |

## Frontend

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 09 | [`09-angular-frontend.md`](./09-angular-frontend.md) | Angular integration — Vercel deployment vs monorepo proxy | 📝 Planned |

---

## System Diagrams

| Diagram | What it shows | SVGs |
|---------|---------------|------|
| **Request Handling Flow** | Browser → Netty → plugin pipeline → router → response lifecycle | [![View](../system_diagrams/(KTor)%20request-handling-flow.svg)](../system_diagrams/(KTor)%20request-handling-flow.svg) |
| **MVC Architecture** | Deployment + component view with example POST /cities lifecycle | [![View](../system_diagrams/(UML)%20openpress-mvc-architecture.svg)](../system_diagrams/(UML)%20openpress-mvc-architecture.svg) |
| **Database Entities (ERD)** | 27 tables, crow's-foot relationships, 7 business domains | [![View](../system_diagrams/(ERD)%20openpress-database-entities.svg)](../system_diagrams/(ERD)%20openpress-database-entities.svg) |
| **UML Class Diagram** | 27 models, 10 repos, 8 services, 8 controllers, 7 plugins, 3 enums | [![View](../system_diagrams/(UML)%20openpress-class-diagram.svg)](../system_diagrams/(UML)%20openpress-class-diagram.svg) |
| **Use Case Overview** | 5 roles × their use cases across the platform | [![View](../system_diagrams/(UML)%20openpress-usecase-overview.svg)](../system_diagrams/(UML)%20openpress-usecase-overview.svg) |
| **Admin Use Case Detail** | Granular admin permissions across 6 domains | [![View](../system_diagrams/(UML)%20openpress-usecase-admin.svg)](../system_diagrams/(UML)%20openpress-usecase-admin.svg) |
| **Login Sequence** | Credentials → AuthService → JWT issue → dashboard or 401 | [![View](../system_diagrams/(UML)%20openpress-sequence-login.svg)](../system_diagrams/(UML)%20openpress-sequence-login.svg) |
| **Checkout Sequence** | Cart → order → stock update → payment → receipt or failure | [![View](../system_diagrams/(UML)%20openpress-sequence-checkout.svg)](../system_diagrams/(UML)%20openpress-sequence-checkout.svg) |
| **Blog Publish Sequence** | Draft → categories/tags → publish → slug generation | [![View](../system_diagrams/(UML)%20openpress-sequence-blog-publish.svg)](../system_diagrams/(UML)%20openpress-sequence-blog-publish.svg) |
| **Admin Theme Sequence** | Admin sets theme → persist → Angular fetches + applies CSS vars | [![View](../system_diagrams/(UML)%20openpress-sequence-admin-theme.svg)](../system_diagrams/(UML)%20openpress-sequence-admin-theme.svg) |

PlantUML source files for all diagrams are available under [`docs/system_diagrams/sketches/`](../system_diagrams/sketches/).

---

## Related Resources

- [README — Project Overview](../../README.md)
- [Project Roadmap](../../README.md#project-roadmap)
- [Quick Start Guide](../../README.md#quick-start)
