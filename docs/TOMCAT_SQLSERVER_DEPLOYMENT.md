# Ledgora CBS — Tomcat + SQL Server Deployment Guide

## Overview

This guide covers deploying `ledgora.war` on Apache Tomcat 10.x with SQL Server.

---

## UAT Connection Details

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | `1433` |
| Database | `ledgora` |
| Username | `sa` |
| Password | `sqlserver#123` |

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
-- Open SQL Server Management Studio (SSMS)
-- Connect to: localhost,1433 with sa / sqlserver#123
-- Select database: ledgora
-- Run: db/migrations/V001__cbs_enhancement_08mar_26.sql
```

Verify it prints `Ledgora CBS Migration V001 — COMPLETE` with no errors.

---

## Step 3 — Configure Tomcat

The `application.properties` already has the UAT defaults baked in, so the
app will connect to SQL Server out of the box **without any extra configuration**
if Tomcat runs on the same machine as SQL Server.

If you need to override (e.g. different host), use one of the options below.

### Option A — `setenv.bat` (Windows — recommended for UAT)

Create `%CATALINA_HOME%\bin\setenv.bat`:

```bat
@echo off
set JAVA_OPTS=%JAVA_OPTS% -Dspring.profiles.active=sqlserver
set JAVA_OPTS=%JAVA_OPTS% -DDB_URL=jdbc:sqlserver://localhost:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true
set JAVA_OPTS=%JAVA_OPTS% -DDB_USERNAME=sa
set JAVA_OPTS=%JAVA_OPTS% "-DDB_PASSWORD=sqlserver#123"
set JAVA_OPTS=%JAVA_OPTS% -DJWT_SECRET=ledgora-uat-jwt-secret-key-minimum-32chars
set JAVA_OPTS=%JAVA_OPTS% -Dledgora.seeder.enabled=false
set JAVA_OPTS=%JAVA_OPTS% -Dlogging.level.com.ledgora=INFO
```

> **Note:** The `#` character in the password must be quoted with double quotes
> on the `-DDB_PASSWORD` line as shown above.

### Option B — `setenv.sh` (Linux/Mac)

Create `$CATALINA_HOME/bin/setenv.sh`:

```bash
#!/bin/bash
export JAVA_OPTS="$JAVA_OPTS \
  -Dspring.profiles.active=sqlserver \
  -DDB_URL='jdbc:sqlserver://localhost:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true' \
  -DDB_USERNAME='sa' \
  -DDB_PASSWORD='sqlserver#123' \
  -DJWT_SECRET='ledgora-uat-jwt-secret-key-minimum-32chars' \
  -Dledgora.seeder.enabled=false \
  -Dlogging.level.com.ledgora=INFO"
```

### Option C — Tomcat `context.xml` (alternative)

In `%CATALINA_HOME%\conf\context.xml`:

```xml
<Context>
    <Environment name="spring.profiles.active"
                 value="sqlserver"
                 type="java.lang.String" override="false"/>
    <Environment name="DB_URL"
                 value="jdbc:sqlserver://localhost:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true"
                 type="java.lang.String" override="false"/>
    <Environment name="DB_USERNAME"
                 value="sa"
                 type="java.lang.String" override="false"/>
    <Environment name="DB_PASSWORD"
                 value="sqlserver#123"
                 type="java.lang.String" override="false"/>
    <Environment name="JWT_SECRET"
                 value="ledgora-uat-jwt-secret-key-minimum-32chars"
                 type="java.lang.String" override="false"/>
</Context>
```

---

## Step 4 — Deploy the WAR

### Windows
```bat
copy target\ledgora.war %CATALINA_HOME%\webapps\ledgora.war
```

### Linux
```bash
cp target/ledgora.war $CATALINA_HOME/webapps/ledgora.war
```

Tomcat auto-deploys. The app will be available at:
`http://localhost:8080/ledgora/`

---

## Step 5 — Verify startup

Check `%CATALINA_HOME%\logs\catalina.out` (Windows) or `$CATALINA_HOME/logs/catalina.out` (Linux):

```
# Good signs:
Ledgora DataInitializer — SKIPPED (ledgora.seeder.enabled=false)
Started LedgoraApplication in X.XXX seconds

# Bad signs — fix before proceeding:
app.jwt.secret must be configured       → JWT_SECRET env var not set
Failed to validate schema               → migration script not run yet
Cannot open server "..." requested by login → wrong DB_URL / credentials
Login failed for user 'sa'              → wrong DB_USERNAME or DB_PASSWORD
```

---

## Key Properties Reference

| Property | Default (UAT) | Purpose |
|----------|--------------|---------|
| `spring.profiles.active` | `sqlserver` | Activates SQL Server dialect |
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=ledgora;...` | JDBC connection |
| `DB_USERNAME` | `sa` | DB login |
| `DB_PASSWORD` | `sqlserver#123` | DB password |
| `JWT_SECRET` | *(must be set)* | Min 32 chars for HS256 |
| `ledgora.seeder.enabled` | `false` | Skips dev data seeding |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema validation only |

---

## SQL Server Connection String Reference

```
# UAT (local, no encryption)
jdbc:sqlserver://localhost:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true

# Remote server (encrypted — recommended for PROD)
jdbc:sqlserver://192.168.1.10:1433;databaseName=ledgora;encrypt=true;trustServerCertificate=false

# Named instance
jdbc:sqlserver://localhost\SQLEXPRESS:1433;databaseName=ledgora;encrypt=false;trustServerCertificate=true
```

---

## Rollback

If the deployment fails:
1. Stop Tomcat: `%CATALINA_HOME%\bin\shutdown.bat`
2. Delete `%CATALINA_HOME%\webapps\ledgora.war` and `webapps\ledgora\` folder
3. Copy the previous WAR back
4. Start Tomcat: `%CATALINA_HOME%\bin\startup.bat`
5. The migration script is additive (no DROP statements) — no DB rollback needed
