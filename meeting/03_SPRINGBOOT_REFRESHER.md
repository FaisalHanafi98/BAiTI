# Spring Boot 3 — Refresher Cheat Sheet

> **Audience**: You (Faisal). You know Laravel deeply. This file translates Laravel concepts to Spring Boot equivalents and gives you the high-density basics you need to talk Spring Boot confidently and write your first endpoint without hesitation.

---

## 1. Mental model: Laravel → Spring Boot

| Laravel | Spring Boot | Notes |
|---|---|---|
| `composer install` | `mvn install` (or `mvn clean install`) | Maven is the build tool. `pom.xml` = `composer.json`. |
| `php artisan serve` | `mvn spring-boot:run` | Starts the embedded Tomcat server. Default port 8080. |
| Routes in `routes/web.php` | `@RestController` annotated classes | No central route file. Routes are defined inline on controller methods. |
| `App\Http\Controllers\UserController` | `UserController` class with `@RestController` | Same idea, different syntax. |
| Eloquent model `User extends Model` | JPA entity `class User { @Id ... }` | Same idea: object ↔ database row. |
| `User::where('email', $x)->first()` | `userRepository.findByEmail(x)` | Repository methods are auto-generated from method names. |
| Middleware | `Filter` or `Interceptor` | Same purpose: run before/after a request. |
| `php artisan make:migration` | Flyway SQL files in `src/main/resources/db/migration/` | Hand-written SQL, run automatically on app start. |
| `.env` file | `application.yml` + `application-dev.yml` + env vars | Profile-based configuration. |
| Service Container / DI | Spring's `@Service` + `@Autowired` (or constructor injection) | Same DI concept, more annotations. |
| Eloquent relationships | `@OneToMany`, `@ManyToOne`, `@ManyToMany` | Annotations on entity fields. |
| Form Request validation | `@Valid` + Bean Validation annotations (`@NotNull`, `@Email`, `@Size`) | Validation lives on the DTO. |
| `php artisan tinker` | Spring Boot DevTools + IDE debugger | No interactive REPL by default. |
| Blade templates | We're not using Blade — frontend is React | N/A |
| `auth()->user()` | `SecurityContextHolder.getContext().getAuthentication()` | More verbose, same concept. |
| `Gate::allows()` | `@PreAuthorize("hasPermission(...)")` | Annotation on the method. |

**Key mental shift**: In Spring Boot, you **annotate** what you want. There's much less manual wiring than Laravel because Spring's container does it for you. The framework feels heavier at first because of the annotations, but once you're past the syntax it's the same MVC + ORM + DI pattern.

---

## 2. Project structure (what lives where)

```
backend/
├── pom.xml                              # Maven config — like composer.json
├── src/
│   ├── main/
│   │   ├── java/my/comunity/            # Your code
│   │   │   ├── MyComunityApplication.java   # Entry point (has main())
│   │   │   ├── config/                  # @Configuration classes
│   │   │   ├── common/                  # Shared utilities, base classes
│   │   │   ├── security/                # JWT, filters, SecurityConfig
│   │   │   ├── tenancy/                 # TenantContext, tenant resolver
│   │   │   ├── property/                # blocks, units (controller/service/repo/entity/dto)
│   │   │   ├── people/                  # users, residents, ...
│   │   │   └── ...
│   │   └── resources/
│   │       ├── application.yml          # Base config
│   │       ├── application-dev.yml      # Dev overrides
│   │       ├── application-prod.yml     # Prod overrides
│   │       └── db/migration/            # Flyway SQL files (V1__..., V2__...)
│   └── test/
│       └── java/my/comunity/            # JUnit 5 tests, mirrors main/
└── target/                              # Compiled output (gitignored)
```

---

## 3. The 4 layers of a feature

For every domain feature (e.g. complaints), you write 4 things:

```
1. Entity        → maps to a database table
2. Repository    → handles DB queries (interface only — Spring writes the impl)
3. Service       → business logic
4. Controller    → HTTP endpoints (REST)
```

Plus 1 or 2 DTOs (request/response shapes — what the frontend sends/receives).

### Example: a Complaint feature

#### 3.1 Entity
```java
@Entity
@Table(name = "complaints")
@Getter @Setter                          // Lombok — generates getters/setters
@NoArgsConstructor @AllArgsConstructor
public class Complaint extends TenantAwareBaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ComplaintCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;
}
```

Annotations decoded:
- `@Entity` = "this class maps to a database table"
- `@Table(name = "complaints")` = "the table is called complaints"
- `@Column(nullable = false)` = `NOT NULL` constraint
- `@ManyToOne` = "many complaints belong to one user/category" (Eloquent's `belongsTo`)
- `@Getter @Setter` = Lombok generates `getTitle()`, `setTitle(...)`, etc. — saves 30 lines of boilerplate per class

#### 3.2 Repository
```java
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    // Spring auto-generates the SQL from the method name
    List<Complaint> findByStatus(ComplaintStatus status);

    Page<Complaint> findBySubmittedBy(User user, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.category.id = :categoryId AND c.status = 'OPEN'")
    List<Complaint> findOpenByCategory(@Param("categoryId") UUID categoryId);
}
```

That's it. **No implementation class needed.** Spring writes the SQL and the impl at runtime. Method-name → SQL is one of Spring's killer features.

#### 3.3 Service
```java
@Service
@RequiredArgsConstructor                 // Lombok — generates constructor with all final fields
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;

    @Transactional
    public Complaint create(CreateComplaintRequest request, User submittedBy) {
        Complaint complaint = new Complaint();
        complaint.setTitle(request.title());
        complaint.setDescription(request.description());
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setSubmittedBy(submittedBy);

        Complaint saved = complaintRepository.save(complaint);
        notificationService.notifyManagersOfNewComplaint(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Complaint> listForUser(User user, Pageable pageable) {
        return complaintRepository.findBySubmittedBy(user, pageable);
    }
}
```

- `@Service` = "this is a Spring-managed bean" — DI container will inject it where needed
- `@Transactional` = wraps the method in a DB transaction (rollback on exception)
- `@RequiredArgsConstructor` = Lombok generates the constructor; Spring uses constructor injection automatically (no `@Autowired` needed)

#### 3.4 Controller
```java
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'COMPLAINT_CREATE')")
    public ResponseEntity<ComplaintResponse> create(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        Complaint created = complaintService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ComplaintResponse.from(created));
    }

    @GetMapping
    public Page<ComplaintResponse> list(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return complaintService.listForUser(currentUser, pageable)
                .map(ComplaintResponse::from);
    }
}
```

- `@RestController` = "every method returns JSON, not a view"
- `@RequestMapping("/api/complaints")` = base URL for this controller
- `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping` = HTTP verbs
- `@Valid` = run Bean Validation on the incoming request body
- `@AuthenticationPrincipal` = inject the logged-in user from Spring Security
- `@PreAuthorize` = method-level permission check (throws 403 if user lacks permission)

#### 3.5 DTOs
```java
// Request — what the frontend sends
public record CreateComplaintRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull UUID categoryId
) {}

// Response — what we send back
public record ComplaintResponse(
        UUID id,
        String title,
        String description,
        String status,
        Instant createdAt
) {
    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getStatus().name(), c.getCreatedAt()
        );
    }
}
```

Java `record` = immutable data class, one line. Use it for DTOs always.

---

## 4. Annotations you'll see daily — the cheat sheet

| Annotation | Used on | Means |
|---|---|---|
| `@SpringBootApplication` | Main class | "This is a Spring Boot app." Includes `@Configuration`, `@ComponentScan`, `@EnableAutoConfiguration`. |
| `@RestController` | Class | This class handles HTTP, returns JSON. |
| `@Service` | Class | Spring-managed business logic class. |
| `@Repository` | Interface | Spring-managed DB access interface. |
| `@Component` | Class | Generic Spring-managed class (use `@Service` or `@Repository` when you can). |
| `@Configuration` | Class | This class defines beans (e.g. SecurityConfig). |
| `@Bean` | Method (inside `@Configuration`) | Defines a single Spring-managed object. |
| `@Autowired` | Field/constructor | Inject a bean. (Prefer constructor injection — Lombok's `@RequiredArgsConstructor` handles it.) |
| `@Entity` | Class | JPA entity (DB table). |
| `@Table(name=...)` | Class | Specify table name. |
| `@Id` | Field | Primary key. |
| `@GeneratedValue` | Field | Auto-generate the ID. |
| `@Column` | Field | DB column config (nullable, length, name). |
| `@OneToMany` `@ManyToOne` `@ManyToMany` | Field | Relationships. |
| `@JoinColumn` | Field | FK column name. |
| `@Transactional` | Method/class | Wrap in DB transaction. |
| `@Valid` | Method param | Validate the incoming object. |
| `@RequestBody` | Method param | Parse the HTTP body as JSON into this object. |
| `@PathVariable` | Method param | Extract from URL: `/users/{id}` → `@PathVariable UUID id` |
| `@RequestParam` | Method param | Extract query string: `?page=2` → `@RequestParam int page` |
| `@PreAuthorize("...")` | Method | Permission check before the method runs. |
| `@AuthenticationPrincipal` | Method param | Inject the logged-in user. |
| `@Profile("dev")` | Class/method | Only active in the `dev` profile. |
| `@Value("${app.foo}")` | Field | Inject from `application.yml`. |

---

## 5. application.yml — config explained

```yaml
spring:
  application:
    name: mycomunity

  datasource:
    url: jdbc:postgresql://localhost:5432/mycomunity
    username: mycomunity
    password: ${DB_PASSWORD}              # From env var
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate                  # Don't auto-create tables — Flyway owns the schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

logging:
  level:
    my.comunity: DEBUG
    org.springframework.security: INFO
```

`application-dev.yml` overrides this for dev (e.g. `show-sql: true`). `application-prod.yml` for prod (e.g. real DB credentials from env vars only).

---

## 6. Security in 30 seconds

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                   // Enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                       // Stateless API, no CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()    // Public
                .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

Flow:
1. Request arrives → `JwtAuthenticationFilter` extracts JWT from `Authorization: Bearer <token>` header
2. Validates the JWT signature
3. Loads the user via `UserDetailsService`
4. Sets `SecurityContextHolder` with the authenticated user
5. The controller method runs; `@PreAuthorize` checks permissions
6. Done

---

## 7. Testing in Spring Boot — the modern way

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ComplaintControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "COMPLAINT_CREATE")
    void create_complaint_returns_201() throws Exception {
        mockMvc.perform(post("/api/complaints")
                .contentType("application/json")
                .content("""
                    {"title":"Lift broken", "description":"...", "categoryId":"..."}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Lift broken"));
    }
}
```

Testcontainers spins up a **real PostgreSQL in Docker** for the test. Way better than mocks. CI runs it automatically.

---

## 8. The 5 commands you'll run all the time

```bash
mvn clean install         # Full build + tests
mvn spring-boot:run       # Start the app (dev profile)
mvn test                  # Just unit tests
mvn verify                # Unit + integration tests
mvn dependency:tree       # Why is this dep here?
```

---

## 9. Common pitfalls (and how to avoid them)

| Pitfall | Fix |
|---|---|
| Forgetting `@Transactional` on a service method that does multiple writes | Always annotate write methods. |
| Loading a lazy relationship outside a transaction → `LazyInitializationException` | Either keep the transaction open longer, or eagerly fetch with `JOIN FETCH` in the query. |
| Returning JPA entities directly from controllers | Always map to DTOs. Entities leak internal structure and cause infinite serialization loops on bidirectional relationships. |
| Using `@Autowired` on fields | Use constructor injection (`@RequiredArgsConstructor`) — easier to test, immutable. |
| Editing an existing Flyway migration file | NEVER. Always add a new `Vn__` file. Editing an applied migration breaks the checksum. |
| Forgetting the `tenant_id` filter on a query | Don't write raw SQL; trust JPA + RLS. |
| Logging sensitive data | Never log passwords, JWTs, or personal data. |

---

## 10. The one-page mental model

**Request comes in** → Spring routes to a `@RestController` method → `@PreAuthorize` checks permissions → method calls a `@Service` → service calls a `@Repository` → repo runs JPA query against PostgreSQL → result flows back up → controller maps entity to DTO → JSON returned.

**That's the whole framework.** Everything else is config and annotations.

---

## 11. If Anep asks you to explain Spring Boot in 60 seconds

> "Spring Boot is Java's web framework. You annotate classes — `@RestController` for HTTP, `@Service` for business logic, `@Repository` for database access, `@Entity` for tables. The framework wires them together via dependency injection. You write your queries by naming methods on a repository interface and Spring generates the SQL for you. Auth is built in via Spring Security with JWT support. Tests run against a real PostgreSQL via Testcontainers. The whole thing compiles to a single JAR you can deploy anywhere with `java -jar`."

That's it. Don't over-explain. He'll get it.
