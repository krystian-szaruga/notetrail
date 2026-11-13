# notetrail

Backend for a notes system. Work in progress.

## Requirements

- JDK 21
- A Compose-compatible runtime (Podman or Docker)

## Local database

```bash
cd infra
cp .env.example .env
podman-compose up -d
```

With Docker:

```bash
cd infra
cp .env.example .env
docker compose up -d
```

Stop it with `podman-compose down` (or `docker compose down`); add `-v` to remove the data volume.

The project is developed on Podman — the author does not use Docker. The same
`infra/docker-compose.yml` works with both.

## Configuration

`infra/.env` variables:

| Variable | Description |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `MYSQL_DATABASE` | Database name |
| `MYSQL_USER` | Application user |
| `MYSQL_PASSWORD` | Application user password |
| `MYSQL_PORT` | MySQL host port |

## Build and tests

```bash
./gradlew build
```

CI (GitHub Actions) runs the same command on JDK 21 for pushes to `main` and pull requests.
