# Spring Boot Tutorial: Customer API

A customer CRUD REST API built step by step as a Spring Boot tutorial. It covers
persistence, JWT security, environment profiles, database migrations, monitoring and testing,
organised as a hexagonal (ports and adapters) application.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.2 |
| Build | Maven (wrapper included) |
| Web | Spring MVC, Bean Validation |
| Persistence | Spring Data JPA, H2 (local/dev/test), PostgreSQL (stg/prod) |
| Migrations | Flyway |
| Security | Spring Security, OAuth2 resource server, JWT (RSA) |
| Monitoring | Actuator, Micrometer, Prometheus |

## Architecture

The project uses **hexagonal architecture** (also called ports and adapters). The business logic sits in
the middle and knows nothing about HTTP, JPA, Spring Security or Micrometer. It talks to the outside world
only through interfaces called **ports**. The technology-specific code lives in **adapters** that plug into those ports.

**Dependency rule:** `infrastructure -> application -> domain`, never the reverse.
The domain and application layers have no dependency on web or database code, so they can be tested
and changed without touching the frameworks.

### Layers

| Layer | Role | In this project |
|---|---|---|
| `domain` | Core business objects and rules, no framework code | `Customer`, `PageQuery`, `PageResult`, `CustomerNotFoundException`, `DuplicateEmailException` |
| `application` | Use cases and the ports they need | Use-case interfaces, output ports, `CustomerService` |
| `infrastructure` | Adapters and framework configuration | Controllers, JPA, metrics, health, security config |

### Ports and adapters

- **Input (driving) ports** (`application.port.in`): what the application offers, such as `CreateCustomerUseCase`
  and `GetCustomerUseCase`. `CustomerService` implements them.
- **Output (driven) ports** (`application.port.out`): what the application needs from the outside, such as
  `CustomerRepositoryPort` (storage) and `CustomerMetricsPort` (metrics).
- **Driving adapters** (`adapter.in`) call the application: `CustomerController` and `AuthController` (REST),
  `HelloWorldRunner` (CLI).
- **Driven adapters** (`adapter.out`) are called by the application: `CustomerPersistenceAdapter` (Spring Data JPA),
  `MicrometerCustomerMetrics` (Micrometer).

### Request flow

```
HTTP request
   │
   ▼
CustomerController            adapter.in.web        maps JSON <-> domain objects
   │  calls an input port
   ▼
CustomerService               application.service   business logic
   │  calls an output port
   ▼
CustomerRepositoryPort        application.port.out  interface
   │  implemented by
   ▼
CustomerPersistenceAdapter    adapter.out.persistence   JPA entity + repository -> database
```

The JPA entity (`CustomerJpaEntity`) and the web DTOs (`CustomerRequest`, `CustomerResponse`) are kept
separate from the domain `Customer`, so database and API details never leak into the core.

### Package layout

```
com.tutorial
├── domain                      Customer, PageQuery, PageResult, domain exceptions
├── application
│   ├── port.in                 Use cases: Create/Get/Update/DeleteCustomerUseCase
│   ├── port.out                CustomerRepositoryPort, CustomerMetricsPort
│   └── service                 CustomerService (implements the use cases)
└── infrastructure
    ├── adapter.in.web          REST controllers, request/response DTOs, GlobalExceptionHandler
    ├── adapter.in.cli          HelloWorldRunner (CommandLineRunner)
    ├── adapter.out.persistence JPA entity, repository, CustomerPersistenceAdapter
    ├── adapter.out.metrics     Micrometer counters and gauge
    ├── adapter.out.health      CustomerStoreHealthIndicator
    ├── config                  SecurityConfig, JwtConfig
    └── security                JwtTokenIssuer
```

## Getting started

Requirements: JDK 25. Maven is not needed, use the included wrapper.

```bash
./mvnw spring-boot:run        # starts on http://localhost:8080 with the "local" profile
./mvnw test                   # runs the test suite
./mvnw package                # builds target/tutorial-0.0.1-SNAPSHOT.jar
```

`.mvn/maven.config` points Maven at an empty `.mvn/settings.xml`, so dependencies resolve
from Maven Central only.

## Profiles

If no profile is set, `local` is used. Select one with `SPRING_PROFILES_ACTIVE` or `--spring.profiles.active`.

| Profile | Database | Notes |
|---|---|---|
| `local` | H2 in memory (`jdbc:h2:mem:tutorial`) | Throwaway data, SQL logged |
| `dev` | H2 file (`./data/tutorial-dev`) | Data survives restarts |
| `stg` | PostgreSQL | Needs env vars below; actuator exposes `health,prometheus` |
| `prod` | PostgreSQL, pool size 20 | Needs env vars below; actuator exposes `health,prometheus`; INFO logging |

`stg` and `prod` have no defaults on purpose, so the app fails fast if one is missing:

```bash
export DB_URL=jdbc:postgresql://host:5432/tutorial
export DB_USERNAME=...
export DB_PASSWORD=...
SPRING_PROFILES_ACTIVE=prod java -jar target/tutorial-0.0.1-SNAPSHOT.jar
```

## Database migrations

Flyway runs the scripts in `src/main/resources/db/migration` automatically at startup.
Hibernate only validates the schema (`ddl-auto: validate`).

| Script | Purpose |
|---|---|
| `V1__create_customers_table.sql` | Creates `customers` (id, name, email) |
| `V2__add_customers_email_index.sql` | Adds an index on email |
| `V3__make_customers_email_unique.sql` | Replaces it with a unique index |

## Security

The API is stateless and uses JWT bearer tokens signed with an RSA key pair.
The key pair is generated at startup, so tokens become invalid after a restart.
Tokens expire after 1 hour by default (`app.security.jwt.ttl`, ISO-8601 duration).

Demo users (in memory, for the tutorial only):

| Username | Password | Role |
|---|---|---|
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

Access rules:

| Request | Access |
|---|---|
| `POST /api/auth/token`, `/actuator/health/**` | Public |
| `GET /api/customers/**` | USER or ADMIN |
| Other `/api/customers/**` (POST, PUT, DELETE) | ADMIN |
| Other `/actuator/**` | ADMIN |

## API

| Method | Path | Description | Success |
|---|---|---|---|
| POST | `/api/auth/token` | Exchange username and password for a JWT | 200 |
| POST | `/api/customers` | Create a customer | 201 + `Location` |
| GET | `/api/customers?page=0&size=20` | Paginated list (`size` 1 to 100) | 200 |
| GET | `/api/customers/{id}` | Get one customer | 200 |
| PUT | `/api/customers/{id}` | Update a customer | 200 |
| DELETE | `/api/customers/{id}` | Delete a customer | 204 |

Request body for create and update: `name` and `email` are required, each at most 255 characters,
and `email` must be valid and unique (stored lower-case).

Errors are returned as RFC 9457 problem details: `400` validation or malformed input, `401` bad or missing
credentials, `403` insufficient role, `404` customer not found, `409` duplicate email, `500` unexpected error.

### Example

```bash
# 1. Get a token (admin can write, user can only read)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .access_token)

# 2. Create a customer
curl -X POST localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace","email":"ada@example.com"}'

# 3. List customers
curl -H "Authorization: Bearer $TOKEN" 'localhost:8080/api/customers?page=0&size=10'
```

## Monitoring

All `/actuator/**` endpoints except health require the ADMIN role.

- `/actuator/health`: public status only; ADMIN also sees details (db, `customerStore`, disk space)
- `/actuator/info`, `/actuator/metrics`
- `/actuator/prometheus`: Prometheus scrape endpoint
- Custom health indicator `customerStore` reports the customer count
- Custom metrics: gauge `customers.count`, counter `customers.operations` tagged `operation=create|update|delete`

## Testing

`./mvnw test` runs everything on H2 with the `test` profile:

| Test | Level |
|---|---|
| `CustomerServiceTest` | Service logic |
| `CustomerPersistenceAdapterTest` | Repository / JPA |
| `CustomerControllerWebMvcTest` | MockMvc controller slice |
| `SecurityTest` | Roles and token rules |
| `CustomerApiIntegrationTest` | Integration |
| `EndToEndTest` | Full flow over HTTP |
| `FlywayMigrationTest`, `ProfileConfigTest`, `ActuatorTest`, `TutorialApplicationTests` | Migrations, profiles, actuator, context load |

## Tutorial progress

| Task | Points | Status |
|---|---|---|
| 1. Hello world with `CommandLineRunner` | 15 | Done |
| 2. Customer CRUD REST API | 20 | Done |
| 3. OAuth2 resource server, JWT, USER/ADMIN roles | 20 | Done |
| 4. Spring profiles local/dev/stg/prod | 10 | Done |
| 5. Flyway migrations (optional) | 10 | Done |
| 6. Actuator, health indicator, Prometheus metrics | 15 | Done |
| 7. Test suite | 10 | Done |
| 8. Waiting for the description | - | Next |

More detail in [docs/TASKS.md](docs/TASKS.md).
