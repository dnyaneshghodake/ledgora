# Ledgora - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- ✅ Java 17 installed
- ✅ Maven 3.9.9 installed
- ✅ Git (optional)

### Step 1: Build the Project (First Time Only)
```bash
cd D:\CBS\ledgora
mvn clean install -DskipTests
```
**Expected Output**: `BUILD SUCCESS`

### Step 2: Run the Application
```bash
java -jar api-gateway/target/ledgora.jar
```

### Step 3: Access the API
```
http://localhost:8080/api
```

---

## 📁 Quick File Reference

| File | Purpose |
|------|---------|
| **README.md** | Complete project documentation |
| **CONFIGURATION_GUIDE.md** | Setup instructions |
| **BUILD_REPORT.md** | Build details & metrics |
| **pom.xml** | Parent Maven configuration |
| **api-gateway/pom.xml** | Main app configuration |

---

## 🛠️ Development Commands

### Build Only (Skip Tests)
```bash
mvn clean package -DskipTests
```

### Run Specific Module
```bash
mvn -pl :account-service spring-boot:run
```

### Check Dependencies
```bash
mvn dependency:tree
```

### Run All Tests
```bash
mvn clean install
```

---

## 📊 Project Structure
```
ledgora/
├── common-lib/              (Shared code)
├── account-service/         (Account operations)
├── transaction-service/     (Credit/Debit)
├── ledger-service/          (Reconciliation)
├── auth-service/            (Authorization)
├── api-gateway/             (Main app - port 8080)
└── target/                  (Build output)
    └── ledgora.jar          (FINAL JAR)
```

---

## 🔗 Service Ports (Development)
- **API Gateway**: 8080 (main entry point)
- **Account Service**: 8081
- **Transaction Service**: 8082
- **Ledger Service**: 8083
- **Auth Service**: 8084

---

## 🧪 Test the API

### Using cURL
```bash
curl http://localhost:8080/api/health
```

### Using Postman
1. Create new GET request
2. URL: `http://localhost:8080/api/health`
3. Send

---

## 🐛 Troubleshooting

### Port 8080 Already in Use
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Build Fails
```bash
mvn clean -U install -DskipTests
```

### JAR Not Found
```bash
ls D:\CBS\ledgora\api-gateway\target\ledgora.jar
```

---

## 📝 Next Steps

1. Configure `application.properties` for each service
2. Add database connection details
3. Implement business logic in services
4. Test endpoints with Postman
5. Develop Angular frontend

---

## 📚 Full Documentation
See **README.md** for complete guide

---

**Quick Start completed!** 🎉

