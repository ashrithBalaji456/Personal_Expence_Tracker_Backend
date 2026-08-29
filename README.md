<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:667eea,100:764ba2&height=220&section=header&text=ExpenseFlow%20API&fontSize=48&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Personal%20Expense%20Tracker%20%7C%20Spring%20Boot%20%2B%20Gemini%20AI&descAlignY=58&descSize=18" />

<a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk" /></a>
<a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg?style=for-the-badge&logo=springboot" /></a>
<a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/Database-PostgreSQL%20%7C%20H2-blue.svg?style=for-the-badge&logo=postgresql" /></a>
<a href="https://personal-expence-tracker-backend.onrender.com"><img src="https://img.shields.io/badge/Deploy-Render-00c1b2.svg?style=for-the-badge&logo=render" /></a>

<img src="https://readme-typing-svg.demolab.com/?font=Fira+Code&weight=600&size=20&pause=1000&color=764ABA&center=true&vCenter=true&width=700&lines=Multi-Tenant+JWT+Authentication;Chronological+Budget+Rollover+Engine;Gemini+Pro+AI+Financial+Advisor;Quota-Safe+Caching+%2B+Graceful+Fallback" />

</div>

> ⚠️ **Note on "animation":** the banner and typing-text lines above are real animated SVGs generated live by two external, community-run services (`capsule-render`, `readme-typing-svg`) — not a GitHub-native feature. If either service ever goes offline or changes its URL scheme, those specific images (not the rest of the README) will simply stop rendering. Everything else below — the diagrams — is standard GitHub-native **Mermaid**, which renders crisp and interactive-feeling (pan/zoom, clickable in the GitHub UI) but is not frame-animated.

---

## 📚 Table of Contents

- [System Architecture](#-system-architecture)
- [Request Lifecycle (Full Trace)](#-request-lifecycle-full-trace)
- [Authentication Flow](#-authentication-flow)
- [Expense CRUD Flow](#-expense-ledger-crud-flow)
- [Budget Rollover Workflow](#-multi-month-carry-over--budget-rollover-workflow)
- [Gemini AI Pipeline](#-secure-gemini-ai-prediction-pipeline-with-quota-fallback)
- [Database Schema (ER Diagram)](#-database-schema)
- [Technology Stack](#️-technology-stack)
- [API Specification](#-endpoint-api-specification)
- [Local Installation](#-local-installation--configuration)
- [Production Deployment](#️-production-deployment-on-render)
- [AI Prompt Engineering](#-gemini-ai-prompt-engineering-details)

---

## 🏗 System Architecture

Layered Controller → Service → Repository pattern, color-coded by responsibility:

```mermaid
graph TD
    Client["🖥️ React Frontend"]
    SecurityFilter["🔐 Spring Security JWT Filter"]
    AuthController["🪪 Auth Controller"]
    Controllers["🎛️ Controllers<br/>Expense · Budget · AI · Analytics"]
    Services["⚙️ Services Layer<br/>Business Logic"]
    Repositories["🗄️ Repositories<br/>Spring Data JPA"]
    GeminiAPI["🤖 Google Gemini Pro API"]
    CachedAlerts[("⚡ In-Memory Cache<br/>10-minute TTL")]
    DB[("🐘 PostgreSQL / H2")]

    Client <-->|HTTP REST / JSON / JWT| SecurityFilter
    SecurityFilter <-->|Authenticate / Authorize| AuthController
    SecurityFilter <-->|Authorized Routes| Controllers
    Controllers -->|DTO Data Transfer| Services
    Services -->|Entity Mapping| Repositories
    Services <-->|HTTP Client / JSON Prompt| GeminiAPI
    Services <-->|Read / Write| CachedAlerts
    Repositories <-->|Spring Data Queries| DB

    classDef client fill:#667eea,stroke:#4c51bf,color:#fff,stroke-width:2px
    classDef security fill:#f56565,stroke:#c53030,color:#fff,stroke-width:2px
    classDef controller fill:#ed8936,stroke:#c05621,color:#fff,stroke-width:2px
    classDef service fill:#48bb78,stroke:#2f855a,color:#fff,stroke-width:2px
    classDef repo fill:#4299e1,stroke:#2b6cb0,color:#fff,stroke-width:2px
    classDef external fill:#9f7aea,stroke:#6b46c1,color:#fff,stroke-width:2px
    classDef cache fill:#ecc94b,stroke:#b7791f,color:#1a202c,stroke-width:2px
    classDef db fill:#38b2ac,stroke:#285e61,color:#fff,stroke-width:2px

    class Client client
    class SecurityFilter security
    class AuthController,Controllers controller
    class Services service
    class Repositories repo
    class GeminiAPI external
    class CachedAlerts cache
    class DB db
```

---

## 🔁 Request Lifecycle (Full Trace)

How one authenticated `POST /api/expenses` call travels through every layer:

```mermaid
sequenceDiagram
    autonumber
    participant U as 🧑 User (Browser)
    participant F as 🌐 React Frontend
    participant J as 🔐 JWT Filter
    participant C as 🎛️ ExpenseController
    participant S as ⚙️ ExpenseService
    participant R as 🗄️ ExpenseRepository
    participant D as 🐘 Database

    U->>F: Fill "Add Expense" form
    F->>J: POST /api/expenses<br/>Authorization: Bearer <token>
    activate J
    J->>J: Validate JWT signature + expiry
    alt Token invalid / expired
        J-->>F: 401 Unauthorized
        F-->>U: ❌ Redirect to login
    else Token valid
        J->>C: Forward request + SecurityContext(userId)
        deactivate J
        activate C
        C->>C: Validate ExpenseRequest DTO
        C->>S: createExpense(userId, dto)
        deactivate C
        activate S
        S->>S: Map DTO → Expense entity
        S->>R: save(expense)
        activate R
        R->>D: INSERT INTO expenses (...)
        activate D
        D-->>R: Row persisted, id generated
        deactivate D
        R-->>S: Expense entity (with id)
        deactivate R
        S-->>C: ExpenseResponse
        deactivate S
        C-->>F: 201 Created + JSON body
        F-->>U: ✅ Expense appears in ledger
    end
```

---

## 🪪 Authentication Flow

```mermaid
sequenceDiagram
    autonumber
    participant U as 🧑 Client
    participant AC as 🪪 AuthController
    participant AS as ⚙️ AuthService
    participant PE as 🔑 PasswordEncoder
    participant UR as 🗄️ UserRepository
    participant JWT as 🎫 JwtUtil
    participant D as 🐘 Database

    Note over U,D: Registration
    U->>AC: POST /api/auth/register
    AC->>AS: registerUser(dto)
    AS->>UR: existsByUsernameOrEmail()
    UR->>D: SELECT ...
    D-->>UR: false (available)
    AS->>PE: encode(rawPassword)
    PE-->>AS: hashedPassword
    AS->>UR: save(newUser)
    UR->>D: INSERT INTO users
    AS->>AS: Seed 10 default budget categories
    AS->>JWT: generateToken(user)
    JWT-->>AS: signed JWT (HS256)
    AS-->>AC: AuthResponse{token, user}
    AC-->>U: 200 OK

    Note over U,D: Login
    U->>AC: POST /api/auth/login
    AC->>AS: authenticate(dto)
    AS->>UR: findByUsernameOrEmail()
    UR->>D: SELECT ...
    D-->>UR: User row
    AS->>PE: matches(raw, hashed)
    alt Password mismatch
        AS-->>AC: throw BadCredentialsException
        AC-->>U: 401 Unauthorized
    else Match
        AS->>JWT: generateToken(user)
        JWT-->>AS: signed JWT
        AS-->>AC: AuthResponse{token, user}
        AC-->>U: 200 OK
    end
```

---

## 💸 Expense Ledger CRUD Flow

```mermaid
flowchart LR
    subgraph Create["➕ Create"]
        A1[POST /api/expenses] --> A2[Validate DTO] --> A3[Persist] --> A4[201 Created]
    end
    subgraph Read["📖 Read"]
        B1[GET /api/expenses] --> B2[Fetch by user_id]
        B3[GET /api/expenses/history?page&size] --> B4[Paginated query]
        B5[GET /api/expenses/category/:cat] --> B6[Filter by category]
    end
    subgraph Update["✏️ Update"]
        C1[PUT /api/expenses/:id] --> C2[Verify ownership] --> C3[Apply changes] --> C4[200 OK]
    end
    subgraph Delete["🗑️ Delete"]
        D1[DELETE /api/expenses/:id] --> D2[Verify ownership] --> D3[Remove row] --> D4[204 No Content]
    end

    classDef create fill:#48bb78,stroke:#2f855a,color:#fff
    classDef read fill:#4299e1,stroke:#2b6cb0,color:#fff
    classDef update fill:#ed8936,stroke:#c05621,color:#fff
    classDef delete fill:#f56565,stroke:#c53030,color:#fff

    class A1,A2,A3,A4 create
    class B1,B2,B3,B4,B5,B6 read
    class C1,C2,C3,C4 update
    class D1,D2,D3,D4 delete
```

---

## 📆 Multi-Month Carry-over & Budget Rollover Workflow

```mermaid
sequenceDiagram
    autonumber
    participant UI as 🖥️ React Client
    participant C as 🎛️ BudgetController
    participant S as ⚙️ BudgetServiceImpl
    participant DB as 🐘 PostgreSQL Database

    UI->>C: GET /api/budget/summary?month=2026-08
    activate C
    C->>S: getBudgetSummary("2026-08")
    activate S
    S->>DB: Get all active months with income/expenses
    DB-->>S: ["2026-06", "2026-07", "2026-08"]
    S->>DB: Query user budget categories
    DB-->>S: [Rent, Groceries, ...]

    Note over S: 🔄 Loop months chronologically to resolve carryovers
    loop for each month prior to targetMonth
        S->>DB: Get resolved income for month
        S->>DB: Get expenses in month
        S->>S: allocated = income × category.percentage
        S->>S: net = allocated − spent
        S->>S: cumulativeBalance += net
    end

    S-->>C: BudgetSummaryResponse (carryovers + net balances)
    deactivate S
    C-->>UI: 200 OK JSON payload
    deactivate C
```

---

## 🤖 Secure Gemini AI Prediction Pipeline with Quota Fallback

```mermaid
flowchart TD
    Start(["🚀 Request AI Alerts / Chat"]) --> CheckAuth["🔐 Authenticate JWT Token"]
    CheckAuth --> FetchData["📊 Compile Financial Context<br/>Income · Budgets · Expenses"]
    FetchData --> CheckCache{"⚡ Cached alerts<br/>< 10 min old?"}

    CheckCache -->|"✅ Yes"| ServeCached["Serve Cached Alerts Instantly"]
    CheckCache -->|"❌ No"| BuildPrompt["📝 Build Contextual Prompt"]

    BuildPrompt --> CallGemini["🤖 Call Gemini Pro REST Endpoint"]
    CallGemini --> CheckResponse{"Response status?"}

    CheckResponse -->|"200 OK"| SaveCache["💾 Save to 10-min Cache"] --> Display["✨ Render Plain-English Alerts"]
    CheckResponse -->|"429 / 500"| CheckOldCache{"Older cache exists?"}

    CheckOldCache -->|"Yes"| ServeOld["⏮️ Serve Older Cache<br/>(graceful degradation)"] --> Display
    CheckOldCache -->|"No"| FallbackAlerts["🛟 Generate Fallback Safety Warnings"] --> Display

    ServeCached --> Display

    classDef entry fill:#667eea,stroke:#4c51bf,color:#fff,stroke-width:2px
    classDef process fill:#4299e1,stroke:#2b6cb0,color:#fff,stroke-width:2px
    classDef decision fill:#ecc94b,stroke:#b7791f,color:#1a202c,stroke-width:2px
    classDef success fill:#48bb78,stroke:#2f855a,color:#fff,stroke-width:2px
    classDef warning fill:#f56565,stroke:#c53030,color:#fff,stroke-width:2px
    classDef final fill:#9f7aea,stroke:#6b46c1,color:#fff,stroke-width:2px

    class Start entry
    class CheckAuth,FetchData,BuildPrompt,CallGemini process
    class CheckCache,CheckResponse,CheckOldCache decision
    class ServeCached,SaveCache success
    class ServeOld,FallbackAlerts warning
    class Display final
```

---

## 💾 Database Schema

```mermaid
erDiagram
    USERS ||--o{ EXPENSES : "logs"
    USERS ||--o{ BUDGET_CATEGORIES : "configures"
    USERS ||--o{ MONTHLY_INCOMES : "receives"

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        timestamp created_at
    }

    EXPENSES {
        bigint id PK
        varchar title
        numeric amount
        varchar category
        date expense_date
        text notes
        timestamp created_at
        timestamp updated_at
        bigint user_id FK
    }

    BUDGET_CATEGORIES {
        bigint id PK
        varchar name
        numeric percentage
        varchar color
        varchar icon
        bigint user_id FK
    }

    MONTHLY_INCOMES {
        bigint id PK
        varchar income_month
        numeric amount
        bigint user_id FK
    }
```

### 🔑 Unique Constraints

| Table | Constraint Name | Enforced Columns | Purpose |
| :--- | :--- | :--- | :--- |
| `users` | `idx_users_username` | `username` | Enforces unique login usernames. |
| `users` | `idx_users_email` | `email` | Prevents registration with duplicate emails. |
| `budget_categories` | `uc_user_category_name` | `user_id, name` | Restricts a user from having duplicate category names. |
| `monthly_incomes` | `uc_user_month` | `user_id, income_month` | Enforces a single salary record per month. |

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| 🧩 Runtime | Java Development Kit (JDK) 17 |
| 🌱 Framework | Spring Boot 3.2.5 (Spring MVC, Spring Security, Spring Data JPA) |
| 🔐 Auth & Security | JSON Web Tokens (JWT) with HS256 algorithm |
| 🗄️ Data Access | Spring Data JPA (Hibernate ORM) |
| 💻 Dev Database | H2 In-Memory/File Database (PostgreSQL mode) |
| 🐘 Prod Database | Deployed PostgreSQL instance |
| 📦 JSON Processing | Jackson ObjectMapper, Spring Web clients |
| 📖 API Docs | OpenAPI 3 / Swagger UI (`springdoc-openapi`) |

---

## 🔗 Endpoint API Specification

All paths are relative to `http://localhost:8080` (Development) or `https://personal-expence-tracker-backend.onrender.com` (Production). Protected routes require the header `Authorization: Bearer <TOKEN>`.

### 1️⃣ Authentication Services (`/api/auth`)
| Method | Path | Description | Access | Request Body | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/register` | Create a new user profile & seed default categories | Public | `RegisterRequest` | `AuthResponse` |
| **POST** | `/login` | Authenticate username/email and password | Public | `LoginRequest` | `AuthResponse` |

### 2️⃣ Expense Ledger Services (`/api/expenses`)
| Method | Path | Description | Access | Parameters | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/` | Fetch all expenses logged by the user | Protected | None | `List<ExpenseResponse>` |
| **GET** | `/history` | Paginated server-side ledger list | Protected | `page`, `size` | `Page<ExpenseResponse>` |
| **POST** | `/` | Log a new transaction expense | Protected | `ExpenseRequest` | `ExpenseResponse` |
| **PUT** | `/{id}` | Edit an existing transaction by ID | Protected | `ExpenseRequest` (Path `id`) | `ExpenseResponse` |
| **DELETE** | `/{id}` | Permanently delete a transaction | Protected | Path `id` | `204 No Content` |
| **GET** | `/category/{category}` | Fetch expenses matching a category string | Protected | Path `category` | `List<ExpenseResponse>` |

### 3️⃣ Budget Envelope Services (`/api/budget`)
| Method | Path | Description | Access | Parameters / Body | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/income` | Log/Update monthly salary | Protected | `MonthlyIncomeRequest` | `MonthlyIncomeResponse` |
| **GET** | `/income` | Fetch income for a target month | Protected | Query `month` (YYYY-MM) | `MonthlyIncomeResponse` |
| **GET** | `/categories` | Fetch category envelope details | Protected | None | `List<BudgetCategoryResponse>` |
| **POST** | `/categories` | Create custom budget category | Protected | `BudgetCategoryRequest` | `BudgetCategoryResponse` |
| **PUT** | `/categories/{id}` | Edit category configurations | Protected | `BudgetCategoryRequest` | `BudgetCategoryResponse` |
| **DELETE** | `/categories/{id}` | Remove a budget category envelope | Protected | Path `id` | `204 No Content` |
| **POST** | `/categories/reset` | Reset all categories to default 10 | Protected | None | `List<BudgetCategoryResponse>` |
| **GET** | `/summary` | Retrieve monthly rollover report | Protected | Query `month` (YYYY-MM) | `BudgetSummaryResponse` |

### 4️⃣ Gemini AI Consulting Services (`/api/ai`)
| Method | Path | Description | Access | Request Body | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/alerts` | Get 2-3 predictive plain-English budget warnings | Protected | None | `List<String>` |
| **POST** | `/chat` | Chat live with the AI Financial Advisor | Protected | `AiChatRequest` | `AiChatResponse` |

---

## 🚀 Local Installation & Configuration

### Prerequisites
* **Java SDK 17** (or above) installed and configured on your path.
* **Apache Maven 3.8+** installed.
* **Git** command line client.

### Step 1 — Clone the Repository
```bash
git clone https://github.com/ashrithBalaji456/Personal_Expence_Tracker_Backend.git
cd Personal_Expence_Tracker_Backend
```

### Step 2 — Configure System Properties
Create a file named `src/main/resources/application.properties` (or edit the existing one):
```properties
# Server port config
server.port=8080

# H2 File Database configuration (Local storage)
spring.datasource.url=jdbc:h2:file:./data/trackerdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Security Configuration
app.jwt.secret=YOUR_TACTILE_SECRET_KEY_MINIMUM_256_BITS_FOR_SECURE_HMAC_SIGNATURES
app.jwt.expiration-ms=86400000

# Gemini API configuration
gemini.api.key=YOUR_GOOGLE_AI_STUDIO_API_KEY
```

> [!IMPORTANT]
> Keep your **Google AI Studio API Key** private. Never commit the raw key to GitHub. Use system environment variables `GEMINI_API_KEY` in production.

### Step 3 — Run the Application
```bash
mvn clean compile
mvn spring-boot:run
```
The server will boot up and bind to `http://localhost:8080`.

---

## ☁️ Production Deployment on Render

### Deploying using Docker

```mermaid
flowchart LR
    A["📦 GitHub Repo"] --> B["🐳 Dockerfile Detected"]
    B --> C["🏗️ Stage 1: Maven Build"]
    C --> D["🏗️ Stage 2: JRE Runtime Image"]
    D --> E["☁️ Render Container Deploy"]
    E --> F["🌍 Live at onrender.com"]

    classDef step fill:#38b2ac,stroke:#285e61,color:#fff,stroke-width:2px
    class A,B,C,D,E,F step
```

The project includes a multi-stage production build `Dockerfile`. Render detects the Dockerfile and automatically builds the container environment:

```dockerfile
# Stage 1: Build the Maven packages
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create execution container
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step-by-Step Render Deployment Guide
1. Log into your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** and select **Web Service**.
3. Link your GitHub repository `https://github.com/ashrithBalaji456/Personal_Expence_Tracker_Backend`.
4. Configure the Web Service settings:
   * **Runtime**: `Docker`
   * **Build Command**: Render handles this via the Dockerfile.
   * **Instance Type**: `Free`
5. Go to the **Environment** tab and add your secrets:
   * `DB_URL`: The URL to your PostgreSQL instance (e.g. `jdbc:postgresql://<host>:<port>/<dbname>`).
   * `DB_USERNAME`: Database username.
   * `DB_PASSWORD`: Database password.
   * `JWT_SECRET`: A secure randomly generated string.
   * `GEMINI_API_KEY`: Your private Google AI Studio API key.
6. Click **Deploy Web Service**. Render will build the container and deploy the app live!

---

## 🤖 Gemini AI Prompt Engineering Details

To keep interactions readable and useful for every user, the backend utilizes custom system instructions when interfacing with the Gemini Pro API:

### 1. Alert Prompt
```text
You are a friendly Budgeting Assistant. Review the user's monthly limits and actual spending:
{Context: Category allocations, Spent amounts, Remaining limits}

Generate exactly 2-3 short, helpful bullet alerts about their spending rules:
1. Use simple, everyday English. Do NOT use terms like 'velocity', 'leverage', or complex jargon.
2. Carefully verify the 'Spent' value for each category. If Spent is 0, they have not spent anything yet. Do NOT warn about overspending in categories with 0 spent. Instead, suggest keeping it that way or congratulate them.
3. Keep each alert short and simple (under 15 words).
4. Do NOT write any introduction or greetings. Just output the bullets.
```

### 2. Chat Prompt
```text
You are a friendly, professional Personal Financial Advisor. Use the following user financial details to answer their query.
{Context: Live ledger overview, category balances, income carry-overs}

Guidelines for your response:
1. Use simple, plain English that is easy to understand. Do NOT use difficult financial terms or complex jargon (avoid words like velocity, projections, amortization, leverage, etc.).
2. Double check the values in the context: 'Spent' is what they have actually spent, and 'Remaining' is what is left. If Spent is 0, they have not spent any money yet. Do not confuse Spent with Remaining or Limit.
3. Provide clear, encouraging, and highly structured advice.

User Query: {UserMessage}
```

---

<div align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:764ba2,100:667eea&height=120&section=footer" />
</div>
