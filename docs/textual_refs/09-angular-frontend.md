# Openpress — Angular Frontend Integration

> **Prerequisites:**
> - Node.js 18+ and Angular CLI installed
> - Openpress API running on `http://localhost:8080`
> - CORS plugin configured (see [`02-plugins-overview.md#21-cors`](./02-plugins-overview.md#21-cors))
> **Status:** 🚧 Planned — The Angular frontend has not been created yet. No `frontend/` directory, Angular project, components, services, or theme API endpoints exist. This document is a guide for implementing the frontend from scratch.

---

## 1. Two Approaches

| Approach | Pros | Cons |
|----------|------|------|
| **A — Separate project + Vercel** | Free hosting, independent deployments, scales separately | Two repos, CORS config, different URLs |
| **B — Monorepo with proxy** | Single repo, same origin (no CORS in dev), unified build | More complex setup, same deployment target |

---

## 2. Approach A: Separate Angular App + Vercel

### 2.1 Create the Angular Project

```bash
# Outside the Openpress project directory
ng new openpress-frontend --routing --style=css
cd openpress-frontend
```

### 2.2 Configure the API Client

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'  // Local dev
};

// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://yourdomain.com'  // Production (see 08-expose-api-to-web.md)
};
```

### 2.3 Create a Service to Call the API

```typescript
// src/app/services/api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    const token = localStorage.getItem('jwt_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    return this.http.get<User[]>(`${this.baseUrl}/users`, { headers });
  }

  createUser(user: User): Observable<number> {
    const token = localStorage.getItem('jwt_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    return this.http.post<number>(`${this.baseUrl}/users`, user, { headers });
  }
}

export interface User {
  id: number;
  email: string;
  password: string;
  authStatus: boolean;
  role: 'ADMIN' | 'AUTHOR' | 'CLERK' | 'USER';
}
```

### 2.4 Build and Deploy to Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Build the Angular app
ng build --configuration production

# Deploy to Vercel
vercel --prod

# Or link to GitHub for auto-deploy:
# 1. Push your Angular project to GitHub
# 2. Import repo at https://vercel.com/new
# 3. Framework preset: Angular
# 4. Build command: ng build --configuration production
# 5. Output directory: dist/openpress-frontend
```

### 2.5 Update CORS in Openpress

Since your frontend and API are on different origins, update the CORS config:

```kotlin
// src/main/kotlin/Http.kt — restrict to your Vercel domain
install(CORS) {
    host("my-app.vercel.app")     // Your Vercel deployment URL
    host("yourdomain.com")         // Custom domain (if set up)
    allowCredentials = true
}
```

---

## 3. Approach B: Monorepo with Angular Proxy

Keep both backend and frontend in the same Git repository.

### 3.1 Create Angular App Inside Openpress

```bash
# From the Openpress project root
ng new frontend --routing --style=css --directory=frontend
```

### 3.2 Project Structure

```
openpress/
├── src/main/kotlin/       # KTor backend
├── frontend/              # Angular app
│   ├── src/
│   ├── proxy.conf.json    # Dev proxy config
│   └── angular.json
├── build.gradle.kts
├── Dockerfile             # Backend Dockerfile
├── docker-compose.yml     # Backend + frontend
└── docs/
```

### 3.3 Configure the Angular Proxy

```json
// frontend/proxy.conf.json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "pathRewrite": { "^/api": "" }
  }
}
```

This means Angular routes like `/api/users` get proxied to `http://localhost:8080/users` during development.

Update the Angular service to use the proxy:

```typescript
// src/app/services/api.service.ts
private baseUrl = '/api';  // Proxy prefix
```

Start Angular with the proxy:

```bash
cd frontend
ng serve --proxy-config proxy.conf.json
# Angular runs on http://localhost:4200
# API calls to /api/* are proxied to http://localhost:8080/*
```

### 3.4 Docker Compose for Production

```yaml
# docker-compose.yml (updated)
version: "3.9"

services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - POSTGRES_URL=jdbc:postgresql://db:5432/openpress
      - POSTGRES_USER=openpress
      - POSTGRES_PASSWORD=changeme
    depends_on:
      db:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - api

  db:
    image: postgres:16-alpine
    # ... (see 06-docker-integration.md)
```

### 3.5 Frontend Dockerfile

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/openpress-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 3.6 Nginx Configuration for Frontend

```nginx
# frontend/nginx.conf
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://api:8080/;
        proxy_set_header Host $host;
    }
}
```

---

## 4. CORS Configuration Summary

| Scenario | CORS Needed? | Config |
|----------|-------------|--------|
| **Angular dev (localhost:4200) → KTor (localhost:8080)** | ✅ Yes | `anyHost()` or `host("localhost:4200")` |
| **Angular prod (Vercel) → KTor (yourdomain.com)** | ✅ Yes | `host("my-app.vercel.app")` |
| **Monorepo with proxy** | ❌ No (same origin) | Not needed |
| **Monorepo Docker Compose** | ❌ No (Nginx proxy) | Not needed |

---

## 5. Which Approach Should You Choose?

| Your Scenario | Recommendation |
|---------------|---------------|
| Want free hosting and independent deploys | **Approach A** — Separate + Vercel |
| Prefer a single repo for everything | **Approach B** — Monorepo with proxy |
| Learning/experimenting | **Approach A** — Simpler to get started |
| Production monolith | **Approach B** — Easier to deploy as one unit |

---

## 6. Theme Configuration API Contract

The admin panel exposes a theme customization endpoint. The Angular frontend fetches this configuration on load and applies it dynamically via CSS custom properties.

### 6.1 Endpoint

| Method | Path | Auth Required | Response |
|--------|------|---------------|----------|
| `GET` | `/api/theme-config` | No | `ThemeConfig` JSON |
| `PUT` | `/admin/theme` | Admin | `200 OK` |

### 6.2 ThemeConfig Shape

```typescript
// src/app/models/theme-config.model.ts
export interface ThemeConfig {
  siteTitle: string;
  siteTagline: string;
  logoUrl: string;
  primaryColor: string;    // e.g. "#3B82F6"
  secondaryColor: string;  // e.g. "#10B981"
  fontFamily: string;      // e.g. "Inter, sans-serif"
  layout: 'sidebar' | 'fullwidth' | 'boxed';
  borderRadius: string;    // e.g. "0.5rem"
}
```

### 6.3 Angular Service

```typescript
// src/app/services/theme.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ThemeConfig } from '../models/theme-config.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  constructor(private http: HttpClient) {}

  async loadTheme(): Promise<void> {
    const config = await firstValueFrom(
      this.http.get<ThemeConfig>(`${environment.apiUrl}/api/theme-config`)
    );
    this.applyTheme(config);
  }

  private applyTheme(config: ThemeConfig): void {
    const doc = document.documentElement;
    doc.style.setProperty('--primary', config.primaryColor);
    doc.style.setProperty('--secondary', config.secondaryColor);
    doc.style.setProperty('--font-family', config.fontFamily);
    doc.style.setProperty('--border-radius', config.borderRadius);
    doc.style.setProperty('--layout', config.layout);
    document.title = config.siteTitle;
    // logo URL can be applied to an <img> tag in the header component
  }
}
```

### 6.4 Bootstrap in AppComponent

```typescript
// src/app/app.component.ts
@Component({ /* ... */ })
export class AppComponent implements OnInit {
  constructor(private theme: ThemeService) {}
  ngOnInit() {
    this.theme.loadTheme();
  }
}
```

### 6.5 CSS Variable Usage

```css
/* src/styles.css */
:root {
  --primary: #3B82F6;           /* fallback */
  --secondary: #10B981;
  --font-family: Inter, sans-serif;
  --border-radius: 0.5rem;
  --layout: fullwidth;
}

body {
  font-family: var(--font-family);
}

.btn-primary {
  background-color: var(--primary);
  border-radius: var(--border-radius);
}
```

### 6.6 PUT /admin/theme Request Body

When the admin saves theme changes, send the same `ThemeConfig` shape:

```typescript
// In admin component
saveTheme(config: ThemeConfig): Observable<void> {
  const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
  return this.http.put<void>(`${this.apiUrl}/admin/theme`, config, { headers });
}
```

---

## 7. Next Steps

1. Create your Angular components (login, dashboard, user management)
2. Implement JWT token storage in `localStorage`
3. Add an HTTP interceptor to attach JWT tokens automatically
4. Implement `ThemeService` to fetch and apply theme config on app bootstrap
5. Build and deploy

Example Angular project structure:

```
frontend/src/app/
├── components/
│   ├── login/
│   ├── dashboard/
│   ├── header/          ← applies logo & site title from ThemeConfig
│   └── users/
├── services/
│   ├── api.service.ts
│   ├── auth.service.ts
│   └── theme.service.ts
├── guards/
│   ├── auth.guard.ts
│   └── role.guard.ts
├── models/
│   ├── user.model.ts
│   └── theme-config.model.ts
└── app-routing.module.ts
```
