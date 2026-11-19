# notetrail

Backend for a notes system.

## Requirements

- JDK 21
- Podman or Docker with Compose (also required by `./gradlew build`: integration tests start MySQL via Testcontainers)

## Running

### Secrets

Copy the example environment file and replace the defaults. `JWT_SECRET` must be at least 32 bytes for HS256, otherwise the application will not start:

```bash
cd infra
cp .env.example .env
openssl rand -hex 32
openssl rand -hex 16
```

Use the first value for `JWT_SECRET` and the second for both `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD`.

### Full stack (database + API)

```bash
cd infra
podman-compose up --build
```

With Docker: `docker compose up --build`.

### Database only

```bash
cd infra
podman-compose up -d mysql
```

The API can then run locally (e.g. from IntelliJ) against `localhost:3306`.

### Application locally

```bash
./gradlew bootRun
```

The data source is read from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and
`DB_PASSWORD`, and tokens are signed with `JWT_SECRET` (at least 32 bytes). When
running from IntelliJ, set these as environment variables in the run
configuration.

### Stopping

```bash
cd infra
podman-compose down
```

Add `-v` to also remove the database volume.

### Logs

Because `up -d` runs in the background, follow the API output with:

```bash
cd infra
podman-compose logs -f api
```

Developed on Podman; the same `infra/docker-compose.yml` works with both.

## API

All endpoints except `/register` and `/login` require `Authorization: Bearer <token>`.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/register` | Create an account (201, 400, 409) |
| `POST` | `/login` | Issue a JWT (200, 400, 401, 429 with `Retry-After`) |
| `POST` | `/items` | Create an item owned by the caller (201) |
| `GET` | `/items` | List owned and shared items with `myRole` (200) |
| `PATCH` | `/items/{id}` | Edit title/content with `version` (200, 400, 403, 404, 409 with current `version`) |
| `DELETE` | `/items/{id}` | Soft delete, owner only (204, 403, 404) |
| `GET` | `/items/{id}/history` | Envers revision history (200, 403, 404) |
| `POST` | `/items/{id}/share` | Grant `VIEWER`/`EDITOR`, owner only (201 new, 200 updated, 400, 403, 404, 409 on concurrent race) |
| `DELETE` | `/items/{id}/share/{userId}` | Revoke access, owner only (204, 403, 404) |

`POST /items/{id}/share` returns 409 when a concurrent share or revoke wins the race; a concurrent first share may also return 200 if the other request committed before this one read the permission.

`DELETE /items/{id}` can also return 409 when the item is modified or deleted concurrently between the read and the save (optimistic locking); retry against the current state.

## Configuration

Variables in `infra/.env`:

| Variable | Description |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | Compose project name (`notetrail`) |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `MYSQL_DATABASE` | Database name |
| `MYSQL_USER` | Application user |
| `MYSQL_PASSWORD` | Application user password |
| `MYSQL_PORT` | MySQL host port |
| `JWT_SECRET` | Secret used to sign JWT tokens |
| `LOGIN_RATE_LIMIT` | Login attempts per minute |

Do not commit real secrets; `.env` files are ignored and only `.env.example` is tracked.

### Application properties

| Property | Default | Description |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:notetrail}` | Built from `DB_HOST`, `DB_PORT`, `DB_NAME` |
| `spring.datasource.username` | `${DB_USER:notetrail}` | From `DB_USER` |
| `spring.datasource.password` | `${DB_PASSWORD}` | From `DB_PASSWORD` |
| `app.jwt.secret` | `${JWT_SECRET}` | HS256 signing key (required, at least 32 bytes) |
| `app.jwt.ttl-seconds` | `3600` | Token lifetime |
| `app.ratelimit.login-capacity` | `${LOGIN_RATE_LIMIT:5}` | Login attempts per minute per IP |
| `app.ratelimit.login-refill-seconds` | `60` | Token bucket refill window |
| `notetrail.security.bcrypt-strength` | `10` | BCrypt cost factor |
| `spring.jpa.hibernate.ddl-auto` | `update` | Envers creates the `*_aud` tables |

## Build and tests

```bash
./gradlew build
```

Docker image is built by Compose from the root `Dockerfile` (multi-stage, JDK 21 → JRE 21).
CI (GitHub Actions) runs `./gradlew build` on JDK 21 and, in a separate `compose` job, starts the full stack with Docker Compose and runs `.github/scripts/smoke-test.sh` against it, on pushes to `main` and on pull requests.

## Architecture

```text
                                           ┌─────────────┐
                                           │   Client    │
                                           └─────────────┘
                                                  ▼
                                           ┌─────────────┐
                                           │  security   │
                                           └─────────────┘
               ┌──────────────────────────────────┴───────────────────────────────┐
               ▼                                                                  ▼
┌─────────────────────────────┐                                    ┌─────────────────────────────┐
│ user                        │                                    │ item                        │
│  ┌───────────────────────┐  │                                    │  ┌───────────────────────┐  │
│  │    adapter/in/web     │  │                                    │  │    adapter/in/web     │  │
│  └───────────────────────┘  │                                    │  └───────────────────────┘  │
│              ▼              │                                    │              ▼              │
│  ┌───────────────────────┐  │      ┌─────────────────────┐       │  ┌───────────────────────┐  │
│  │      application      │  │◄─────│ bootstrap           │◄──────│  │      application      │  │
│  └───────────────────────┘  │      │ UserLookup →        │       │  └───────────────────────┘  │
│              ▼              │      │ FindUserUseCase     │       │              ▼              │
│  ┌───────────────────────┐  │      └─────────────────────┘       │  ┌───────────────────────┐  │
│  │        domain         │  │                                    │  │        domain         │  │
│  └───────────────────────┘  │                                    │  └───────────────────────┘  │
│              ▲              │                                    │              ▲              │
│  adapter/out/persistence    │                                    │  adapter/out/persistence    │
└─────────────────────────────┘                                    └─────────────────────────────┘
               │                                                                  │
               └──────────────────────────────────┬───────────────────────────────┘
                                                  ▼
                                           ┌─────────────┐
                                           │    MySQL    │
                                           └─────────────┘
```

`item` uses `security.CurrentUser` in its Envers revision listener, and `user` receives the `SecretKey` bean from `security`; `security` never depends on either feature module.

`bootstrap` (root package) is the only place that connects the modules: it adapts `item`'s `UserLookup` port to `user`'s `FindUserUseCase`. Neither module imports the other; ArchUnit enforces this.

If `item` ever has to react to changes in `user` (for example account deletion), the next step would be application/domain events backed by a message bus, instead of widening `UserLookup` with more synchronous calls. The current scope does not need it.

- Hexagonal per feature; `domain` and `application` are framework-free.
- `item` never imports `user`; the only link is the `UserLookup` port wired in `bootstrap`.
- `security` is a shared kernel: it verifies JWTs and depends only on `core`; feature modules may use its `CurrentUser` context and the `SecretKey` bean. ArchUnit enforces the feature-module and layer boundaries; the shared-kernel dependency is deliberate.
- `ItemJpaEntity` is `@Audited`, uses `@Version` for optimistic locking and `deleted` for soft delete; history is read through `AuditReader`.

## Design decisions

| Decision | Why | Rejected |
| --- | --- | --- |
| Hexagonal, organized by feature | Core rules stay framework-free, unit tests need no context | Layer-first packages |
| JJWT 0.13 for tokens | Signing + `exp` validation in a few lines | Nimbus (needs JWK/JWE), Spring Security OAuth2 |
| Validation: DTO presence, domain invariants, service password policy | DTO mirrors the domain limits for an early 400; the domain remains the source of truth. | All validation in the DTO |
| No explicit transaction boundary in services | Every operation ends with a single write, so the repository call provides the transaction. `@Version` guards item updates (409), `unique(item_id, user_id)` guards concurrent first shares (409), and a share that races a revoke returns 409 when `updateRole` no longer finds the permission. | A custom `TransactionRunner` port with a `TransactionTemplate` adapter adds an abstraction that buys nothing while every business operation is a single write. |
| Concurrent first share returns 200 or 409 | Two simultaneous first shares for the same user both see no permission. The insert that commits first creates the permission (201); the other either hits `unique(item_id, user_id)` and returns 409 ("retry"), or reads after the first commit and updates the existing row with 200. Both outcomes are valid; `created` comes from the insert result, never from the earlier read. This relies on the share flow having no surrounding transaction (see the previous row). | Forcing a single status with an explicit lock or retry loop, which adds code the spec does not require |
| Explicit `deleted = false` in item queries | `@Query` JPQL in `ItemJpaRepository` keeps the soft-delete filter visible in the code | Derived method names (`...AndDeletedFalse`) hide the filter behind name parsing |
| Soft delete is audited as `MOD` | Setting `deleted = true` is a normal Hibernate update, so Envers records `MOD` and `items_aud` keeps every content revision, including the final state; the history endpoint still returns 404 for deleted items. | Physical delete (`DEL`), `@SQLDelete` / native updates that bypass Envers |
| 404 before 403 | The spec requires 404 for missing or deleted items and 403 for foreign ones. The item is loaded first (`deleted = false`), then permissions are checked. This reveals that a foreign item exists, which is accepted because ids are random UUIDs. | Returning 404 for foreign items as well (hides existence, but contradicts the spec) |
| In-memory rate limiter | Bucket4j token bucket per instance, no external store, configuration via `LOGIN_RATE_LIMIT` | Redis-backed buckets (the task forbids Redis; a single instance needs no shared store) |
| `ddl-auto=update` instead of Flyway | Envers generates the `*_aud` tables automatically; the task forbids writing them by hand | Flyway with `validate` would require hand-written audit DDL |
| Re-granting an existing share keeps `grantedAt` | Overwriting a role is an update, not a new grant: only `role` changes and the original grant timestamp is preserved (200) | Inserting a new permission on every share (loses the original grant time and contradicts the update semantics) |
| Plain `UUID` columns, no database FK constraints | The spec models them as references and the application enforces them: the owner comes from the JWT, the share target is checked by `UserLookup` (404, spec 4.8), users are never deleted and items are soft-deleted, so no orphan can appear. | Database FK constraints: they would only guard what the code already guards |

## License

MIT — see [LICENSE](LICENSE).

