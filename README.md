# notetrail

Backend for a notes system. Work in progress.

## Requirements

- JDK 21
- Podman or Docker with Compose

## Running

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
`DB_PASSWORD`. When running from IntelliJ, set these as environment variables in
the run configuration.

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

The project is developed on Podman — the author does not use Docker. The same
`infra/docker-compose.yml` works with both.

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

## Build and tests

```bash
./gradlew build
```

Docker image is built by Compose from the root `Dockerfile` (multi-stage, JDK 21 → JRE 21).
CI (GitHub Actions) runs `./gradlew build` on JDK 21 for pushes to `main` and pull requests.
