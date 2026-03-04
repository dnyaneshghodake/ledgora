# LEDGORA - Core Banking Solutions Backend

## 📋 Project Overview

**Ledgora** is a microservices-based core banking solution built with **Spring Boot 4.0.3** and **Java 17**. The system provides comprehensive banking services through a multi-module Maven architecture with an Angular frontend.

### Key Information
- **Version**: 0.0.1-SNAPSHOT
- **Java Version**: 17
- **Spring Boot**: 4.0.3
- **Maven**: 3.9.9
- **Final Artifact**: `ledgora.jar` (Located in `api-gateway/target/`)
- **Deployment Type**: Executable JAR (not WAR)

---

## 📁 Project Structure

```
ledgora/
│
├── pom.xml (Parent POM - Multi-module)
├── common-lib/                  # Shared Library
│   ├── pom.xml
│   └── src/main/java/com/ledgora/
│
├── account-service/             # Account Management Service
│   ├── pom.xml
│   └── src/main/java/com/ledgora/
│
├── transaction-service/         # Transaction Processing Service
│   ├── pom.xml
│   └── src/main/java/com/ledgora/
│
├── ledger-service/             # Reconciliation & Settlement Service
│   ├── pom.xml
│   └── src/main/java/com/ledgora/
│
├── auth-service/               # Authentication & Authorization Service
│   ├── pom.xml
│   └── src/main/java/com/ledgora/
│
├── api-gateway/                # API Gateway & Main Application
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/ledgora/
│   │   │   ├── LedgoraApplication.java (Main Entry Point)
│   │   │   ├── GatewayController.java
│   │   │   └── ServletInitializer.java
│   │   └── resources/
│   │       └── application.properties
│   └── target/
│       └── ledgora.jar         # FINAL ARTIFACT
│
├── PROJECT_SETUP_SUMMARY.md    # Comprehensive Project Documentation
├── CONFIGURATION_GUIDE.md      # Configuration Instructions
├── build.bat                   # Windows Build Script
├── build.sh                    # Linux/Mac Build Script
├── HELP.md                     # Spring Boot Help
└── mvnw/mvnw.cmd              # Maven Wrapper
```

---

## 🏗️ Module Architecture

### 1. **common-lib** - Shared Library Foundation
```
Purpose: Centralized dependencies and shared utilities
Type: JAR Library (not executable)
Scope: Used by all microservices
Dependencies: Base Spring Boot, Database, Security, Utilities
```

### 2. **account-service** - Account Management Microservice
```
Port: 8081 (suggested)
Purpose: Account opening, management, and operations
Features:
  - Account creation
  - Account modification
  - Account closure
  - Account inquiry
API Endpoints: /api/accounts/*
Depends on: common-lib
```

### 3. **transaction-service** - Transaction Processing Microservice
```
Port: 8082 (suggested)
Purpose: Handle all debit/credit transactions
Features:
  - Debit transactions
  - Credit transactions
  - Transaction validation
  - Transaction history
API Endpoints: /api/transactions/*
Depends on: common-lib
```

### 4. **ledger-service** - Reconciliation & Settlement Service
```
Port: 8083 (suggested)
Purpose: Settlement and reconciliation of transactions
Features:
  - Transaction settlement
  - Balance reconciliation
  - End-of-day reconciliation
  - Ledger entries
API Endpoints: /api/ledger/*
Depends on: common-lib
```

### 5. **auth-service** - Security & Authorization Service
```
Port: 8084 (suggested)
Purpose: Authentication and authorization management
Features:
  - User authentication
  - JWT token generation/validation
  - Role-based access control
  - Security policies
API Endpoints: /api/auth/*
Depends on: common-lib
```

### 6. **api-gateway** - API Gateway & Main Application
```
Port: 8080 (default)
Purpose: Single entry point for all requests
Features:
  - Request routing to microservices
  - Load balancing
  - API aggregation
  - CORS handling
  - Request/response filtering
API Endpoints: /api/* (routes to respective services)
Depends on: common-lib + all other services
Final Artifact: ledgora.jar (EXECUTABLE)
```

---

## 🚀 Build & Deployment

### Building the Project

#### Option 1: Full Build with Install
```bash
mvn clean install -DskipTests
```

#### Option 2: Package Only (No Install)
```bash
mvn clean package -DskipTests
```

#### Option 3: Build Specific Module
```bash
mvn clean package -pl :api-gateway -DskipTests
```

#### Option 4: Using Build Scripts
```bash
# Windows
./build.bat

# Linux/Mac
./build.sh
```

### Build Output
- ✅ **Location**: `api-gateway/target/ledgora.jar`
- ✅ **Size**: ~70-80 MB (includes all dependencies)
- ✅ **Type**: Executable JAR with embedded server
- ✅ **No deployment server needed** (built-in Tomcat)

---

## 🎯 Running the Application

### Run the JAR File
```bash
java -jar api-gateway/target/ledgora.jar
```

### Run with Custom Port
```bash
java -jar api-gateway/target/ledgora.jar --server.port=8090
```

### Run with Spring Profile
```bash
java -jar api-gateway/target/ledgora.jar --spring.profiles.active=production
```

### Run Individual Modules (Development)
```bash
# Terminal 1 - Auth Service
mvn -pl :auth-service spring-boot:run

# Terminal 2 - Account Service
mvn -pl :account-service spring-boot:run

# Terminal 3 - Transaction Service
mvn -pl :transaction-service spring-boot:run

# Terminal 4 - Ledger Service
mvn -pl :ledger-service spring-boot:run

# Terminal 5 - API Gateway
mvn -pl :api-gateway spring-boot:run
```

---

## 📋 Dependencies Overview

### Parent POM (ledgora)
- Spring Boot Parent: 4.0.3
- Java Version: 17
- Dependency Management: Centralized

### common-lib Dependencies (to be added)
- Spring Boot Starters (Web, Data JPA)
- Database Drivers (MySQL, H2)
- Security (Spring Security, JWT)
- Validation (Bean Validation)
- Logging (SLF4J, Logback)
- API Documentation (Springdoc OpenAPI)
- Utility (Lombok)

### Service Dependencies
- common-lib (all services)
- Spring Boot Web Starter (microservices)
- Spring Boot Maven Plugin (for executable JARs)

---

## 🔧 Configuration

### Environment Setup
1. **Java 17** - Ensure PATH is set
2. **Maven 3.9.9+** - Ensure PATH is set
3. **Database** - MySQL or H2 (for development)
4. **IDE** - IntelliJ IDEA, Eclipse, or VS Code

### Port Configuration
```properties
# Add to each service's application.properties
server.port=8081  # Change as per service

# For api-gateway
server.port=8080
```

### Database Configuration
```properties
# Spring Data JPA
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mysql://localhost:3306/ledgora_db
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### CORS for Angular
```java
// Add to api-gateway
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("*");
    }
}
```

---

## 🔐 Security Considerations

1. **Authentication**: Implement JWT-based authentication in auth-service
2. **Authorization**: Use role-based access control (RBAC)
3. **API Gateway Routing**: Validate all requests through gateway
4. **Database Security**: Use parameterized queries
5. **HTTPS**: Enable TLS/SSL for production
6. **Environment Variables**: Store sensitive data in env vars

---

## 🌐 Integration with Angular Frontend

### Frontend Configuration
```typescript
// environment.ts
export const environment = {
  apiUrl: 'http://localhost:8080/api'
};
```

### Service Communication
```typescript
// service.ts
constructor(private http: HttpClient) {}

getAccounts() {
  return this.http.get(`${this.apiUrl}/accounts`);
}
```

### CORS Handling
- API Gateway should allow requests from Angular dev server (port 4200)
- Production: Configure for your domain

---

## 📊 API Endpoints Structure

```
API Gateway (Port 8080)
│
├── /api/accounts/*           → Account Service (8081)
├── /api/transactions/*       → Transaction Service (8082)
├── /api/ledger/*            → Ledger Service (8083)
├── /api/auth/*              → Auth Service (8084)
└── /api/health              → Health Check
```

---

## 🧪 Testing

### Build with Tests
```bash
mvn clean install
```

### Skip Tests (Faster Build)
```bash
mvn clean install -DskipTests
```

### Run Tests for Specific Module
```bash
mvn test -pl :account-service
```

---

## 📦 Maven Commands Reference

| Command | Purpose |
|---------|---------|
| `mvn clean` | Remove all compiled files |
| `mvn compile` | Compile source code |
| `mvn test` | Run unit tests |
| `mvn package` | Create JAR/WAR file |
| `mvn install` | Install to local repository |
| `mvn deploy` | Deploy to remote repository |
| `mvn clean install` | Clean + Compile + Test + Package |
| `mvn clean package -DskipTests` | Quick build without tests |

---

## 🔍 Troubleshooting

### Issue: Build Fails with Compilation Errors
**Solution:**
```bash
mvn clean install -U  # Update dependencies
mvn dependency:tree   # Check for conflicts
```

### Issue: Port Already in Use
**Solution (Windows):**
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Issue: JAR Not Created
**Solution:**
```bash
mvn clean package -DskipTests -X  # Debug mode
```

### Issue: Dependency Not Found
**Solution:**
```bash
# Check Maven repository
mvn dependency:resolve
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-compiler-plugin
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/docs/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [Swagger/OpenAPI](https://swagger.io/)

---

## ✅ Build Status

```
✓ Parent POM: CONFIGURED
✓ common-lib: BUILD SUCCESS
✓ account-service: BUILD SUCCESS
✓ transaction-service: BUILD SUCCESS
✓ ledger-service: BUILD SUCCESS
✓ auth-service: BUILD SUCCESS
✓ api-gateway: BUILD SUCCESS
✓ Final Artifact: ledgora.jar CREATED
```

---

## 📝 Next Steps

1. ✅ Multi-module structure configured
2. ✅ Project builds successfully
3. ⏭️ **Add common dependencies** to common-lib
4. ⏭️ **Implement service interfaces** in each module
5. ⏭️ **Configure database** connections
6. ⏭️ **Create REST controllers** for each service
7. ⏭️ **Implement authentication** in auth-service
8. ⏭️ **Setup API gateway routing** rules
9. ⏭️ **Develop Angular frontend**
10. ⏭️ **Deploy to production**

---

## 👨‍💻 Development Workflow

```
1. Clone/Setup Project
   ↓
2. Build Project (mvn clean install)
   ↓
3. Run Individual Services
   ↓
4. Test with Postman/Insomnia
   ↓
5. Develop Angular Frontend
   ↓
6. Test Frontend + Backend Integration
   ↓
7. Deploy ledgora.jar to server
```

---

## 🚀 Deployment Checklist

- [ ] Update application properties for production
- [ ] Configure database connection
- [ ] Set up logging
- [ ] Enable HTTPS/TLS
- [ ] Configure environment variables
- [ ] Set appropriate JVM parameters
- [ ] Test all endpoints
- [ ] Setup monitoring/alerting
- [ ] Backup database
- [ ] Deploy ledgora.jar

---

**Last Updated:** 2026-03-04  
**Project Version:** 0.0.1-SNAPSHOT  
**Status:** ✅ Ready for Development

