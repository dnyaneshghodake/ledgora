# Ledgora CBS — Tomcat + SQL Server Deployment Guide

## Overview

This guide covers deploying `ledgora.war` on Apache Tomcat 10.x with SQL Server.

---

## Prerequisites

| Component | Version |
|-----------|---------|
| Java | 17 (Temurin/Corretto) |
| Apache Tomcat | 10.1.x |
| SQL Server | 2019 or 2022 |
| Maven | 3.9+ (build only) |

---

## Step 1 — Build the WAR

```bash
mvn clean package -DskipTests
# Output: target/ledgora.war
```

---

## Step 2 — Run the DB migration script

Before deploying the WAR, run the migration on the UAT SQL Server:

```sql
-- Connect as DBO or db_owner
-- Run: db/migrations/V001__cbs_enhancement_08mar_26.sql
```

Verify it prints `Ledgora CBS Migration V001 — COMPLETE` with no errors.

---

## Step 3 — Configure Tomcat environment variables

### Option A — `setenv.sh` (Linux/Mac)

Create `$CATALINA_HOME/bin/setenv.sh`:

```bash
#!/bin/bash
export JAVA_OPTS="$JAVA_OPTS \
  -Dspring.profiles.active=sqlserver \
  -DDB_URL='jdbc:sqlserver://YOUR_HOST:1433;databaseName=ledgora;encrypt=true;trustServerCertificate=false' \
  -DDB_USERNAME='ledgora_app' \
  -DDB_PASSWORD='YOUR_STRONG_PASSWORD' \
  -DJWT_SECRET='YOUR_MINIMUM_32_CHAR_SECRET_HERE' \
  -Dledgora.seeder.enabled=false \
  -Dlogging.level.com.ledgora=INFO"
```

### Option B — `setenv.bat` (Windows)

Create `%CATALINA_HOME%\bin\setenv.bat`:

```bat
set JAVA_OPTS=%JAVA_OPTS% -Dspring.profiles.active=sqlserver
set JAVA_OPTS=%JAVA_OPTS% -DDB_URL=jdbc:sqlserver://YOUR_HOST:1433;databaseName=ledgora;encrypt=true;trustServerCertificate=false
set JAVA_OPTS=%JAVA_OPTS% -DDB_USERNAME=ledgora_app
set JAVA_OPTS=%JAVA_OPTS% -DDB_PASSWORD=YOUR_STRONG_PASSWORD
set JAVA_OPTS=%JAVA_OPTS% -DJWT_SECRET=YOUR_MINIMUM_32_CHAR_SECRET_HERE
set JAVA_OPTS=%JAVA_OPTS% -Dledgora.seeder.enabled=false
set JAVA_OPTS=%JAVA_OPTS% -Dlogging.level.com.ledgora=INFO
```

### Option C — Tomcat `context.xml` (per-application)

In `$CATALINA_HOME/conf/context.xml` or `webapps/ledgora/META-INF/context.xml`:

```xml
<Context>
    <Environment name="spring.profiles.active" value="sqlserver" type="java.lang.String" override="false"/>
    <Environment name="DB_URL"      value="jdbc:sqlserver://HOST:1433;databaseName=ledgora;encrypt=true" type="java.lang.String" override="false"/>
    <Environment name="DB_USERNAME" value="ledgora_app" type="java.lang.String" override="false"/>
    <Environment name="DB_PASSWORD" value="YOUR_STRONG_PASSWORD" type="java.lang.String" override="false"/>
    <Environment name="JWT_SECRET"  value="YOUR_MINIMUM_32_CHAR_SECRET_HERE" type="java.lang.String" override="false"/>
</Context>
```

---

## Step 4 — Deploy the WAR

```bash
cp target/ledgora.war $CATALINA_HOME/webapps/ledgora.war
```

Tomcat auto-deploys. The app will be available at:
`http://YOUR_HOST:8080/ledgora/`

---

## Step 5 — Verify startup

Check `$CATALINA_HOME/logs/catalina.out`:

```
# Good signs:
Ledgora DataInitializer — SKIPPED (ledgora.seeder.enabled=false)
Started LedgoraApplication in X.XXX seconds

# Bad signs (fix before going live):
app.jwt.secret must be configured
Failed to validate schema
```

---

## Key Properties Reference

| Property | Where to set | Purpose |
|----------|-------------|---------|
| `spring.profiles.active=sqlserver` | `setenv.sh` / `-D` | Activates SQL Server dialect + validate DDL |
| `DB_URL` | `setenv.sh` / `-D` | SQL Server JDBC URL |
| `DB_USERNAME` | `setenv.sh` / `-D` | DB login |
| `DB_PASSWORD` | `setenv.sh` / `-D` | DB password (never commit) |
| `JWT_SECRET` | `setenv.sh` / `-D` | Min 32 chars, used for session tokens |
| `ledgora.seeder.enabled=false` | `setenv.sh` / `-D` | Skip dev data seeding on UAT/PROD |
| `spring.jpa.hibernate.ddl-auto=validate` | `application-sqlserver.properties` | Schema validation only — no DDL changes |

---

## SQL Server Connection String Examples

```
# Basic (no encryption — internal network only)
jdbc:sqlserver://192.168.1.10:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true

# Encrypted (recommended for UAT/PROD)
jdbc:sqlserver://db.internal:1433;databaseName=ledgora;encrypt=true;trustServerCertificate=false

# Named instance
jdbc:sqlserver://db.internal\\SQLEXPRESS;databaseName=ledgora;encrypt=false
```

---

## Rollback

If the deployment fails:
1. Stop Tomcat
2. Restore the previous `ledgora.war`
3. The migration script is additive (no DROP statements) — no DB rollback needed for this release
4. Start Tomcat
