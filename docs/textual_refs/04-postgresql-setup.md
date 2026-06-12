# Openpress — PostgreSQL Setup Guide

> **Prerequisites:** Administrative access to your machine (sudo/admin rights)
> **Note:** By default, Openpress runs with an **embedded H2 database** in development mode (`embedded = true`). The PostgreSQL configuration described below is for **production use only**. You can develop without installing PostgreSQL — H2 works out of the box.

---

## 1. Install PostgreSQL

### Arch Linux (pacman)

```bash
sudo pacman -S postgresql
sudo systemctl start postgresql
sudo systemctl enable postgresql  # Auto-start on boot
```

### Debian / Ubuntu (apt)

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### CentOS / RHEL (dnf)

```bash
sudo dnf install postgresql-server postgresql-contrib
sudo postgresql-setup --initdb     # Initialize database cluster
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Windows

**Option A — Docker (recommended):**

```bash
docker run -d \
  --name openpress-db \
  -e POSTGRES_DB=openpress \
  -e POSTGRES_USER=openpress \
  -e POSTGRES_PASSWORD=your_secure_password \
  -p 5432:5432 \
  postgres:16
```

**Option B — Native installer:**

1. Download from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
2. Run the installer — note the **port** (default 5432) and **password** you set
3. Add `C:\Program Files\PostgreSQL\16\bin` to your `PATH`
4. Start the service: `pg_ctl start`

---

## 2. Create the Database and User

```bash
# Switch to the postgres system user
sudo -u postgres psql
```

```sql
-- Inside psql shell

CREATE DATABASE openpress;

CREATE USER openpress WITH ENCRYPTED PASSWORD 'your_secure_password';

GRANT ALL PRIVILEGES ON DATABASE openpress TO openpress;

-- Connect to the database to grant schema permissions
\c openpress

GRANT ALL ON SCHEMA public TO openpress;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO openpress;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO openpress;

\q
```

---

## 3. Configure Openpress to Use PostgreSQL

### 3.1 Update `application.yaml`

Add the PostgreSQL connection properties:

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - org.nexus.openpress.HttpKt.configureHttp
      - org.nexus.openpress.MonitoringKt.configureMonitoring
      - org.nexus.openpress.SerializationKt.configureSerialization
      - org.nexus.openpress.SecurityKt.configureSecurity
      - org.nexus.openpress.PostgresKt.configurePostgres
      - org.nexus.openpress.RoutingKt.configureRouting

postgres:
  url: "jdbc:postgresql://localhost:5432/openpress"
  user: "openpress"
  password: "your_secure_password"
```

### 3.2 Update `Postgres.kt`

The project's `connectToPostgres()` function already supports reading these properties. Pass `embedded = false`:

```kotlin
fun Application.configurePostgres() {
    val dbConnection: Connection = connectToPostgres(embedded = false)  // ← switch to false
    // ... rest stays the same
}
```

```kotlin
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        // H2 embedded — for tests
        DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()
        DriverManager.getConnection(url, user, password)
    }
}
```

### 3.3 Keep H2 for Tests (Optional)

You can keep the `embedded = true` fallback for local development without installing PostgreSQL. The `connectToPostgres()` function defaults to H2 when `embedded = true`.

---

## 4. Verify the Connection

```bash
# Run the application
./gradlew run
```

Expected log output:

```
INFO  Application - Connecting to postgres database at jdbc:postgresql://localhost:5432/openpress
INFO  Application - Responding at http://0.0.0.0:8080
```

If you see a connection error, check:

- Is PostgreSQL running? `sudo systemctl status postgresql`
- Is the port correct? `ss -tlnp | grep 5432`
- Are the credentials correct? Try connecting manually: `psql -U openpress -d openpress -h localhost`

---

## 5. Environment Variables (Recommended for Production)

Instead of hardcoding credentials in `application.yaml`, use environment variables:

```yaml
postgres:
  url: ${POSTGRES_URL}
  user: ${POSTGRES_USER}
  password: ${POSTGRES_PASSWORD}
```

Run with:

```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/openpress \
POSTGRES_USER=openpress \
POSTGRES_PASSWORD=your_secure_password \
./gradlew run
```

---

## 6. Troubleshooting

| Issue | Solution |
|-------|----------|
| `FATAL: Ident authentication failed` | Edit `pg_hba.conf`, change `peer` to `md5` for local connections |
| `FATAL: database "openpress" does not exist` | Create it: `CREATE DATABASE openpress;` |
| `Connection refused` | Ensure PostgreSQL is running: `sudo systemctl start postgresql` |
| Port 5432 already in use | Change port in `postgresql.conf` and update `postgres.url` in `application.yaml` |

### Finding `pg_hba.conf`

```bash
# Locate the config file
sudo -u postgres psql -c "SHOW hba_file;"
```

Example path on Arch: `/var/lib/postgres/data/pg_hba.conf`
Example path on Debian: `/etc/postgresql/16/main/pg_hba.conf`
