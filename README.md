# Cloud CI/CD Lab — Spring Boot + PostgreSQL

Portfolio project for a DevOps Intern: REST API, Docker Compose, GitHub Actions CI, GHCR image publishing, Ubuntu deployment and health verification.

## 1. Run locally (Windows PowerShell)

Requirements: Docker Desktop with WSL2 backend, Git; Java/Maven not necessary for Docker run.

```powershell
Copy-Item .env.example .env
# Edit .env and replace DB_PASSWORD with a strong password
 docker compose up --build -d
 docker compose ps
 curl.exe http://localhost:8080/actuator/health
 curl.exe -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" -d '{"title":"First deploy"}'
 curl.exe http://localhost:8080/api/tasks
```

Stop without deleting database: `docker compose down`. Destructive reset: `docker compose down -v`.

## 2. CI/CD milestones

- `.github/workflows/ci.yml`: run Maven unit tests and Docker build on main pushes and PRs.
- `.github/workflows/publish.yml`: build/push `ghcr.io/OWNER/REPO:latest` and a SHA tag when main is pushed.
- Both workflows are *independent*: publishing does not currently gate on successful CI. For production, connect them with a reusable workflow or job dependency.

## 3. Ubuntu VM deployment (manual and intentional)

1. Install Docker Engine + Compose plugin and nginx on Ubuntu. Allow SSH/HTTP in the cloud firewall. Run Docker as a user with appropriate permissions.
2. Copy `deploy/compose.yaml` and `deploy/deploy.sh` to `~/cloud-cicd-lab/deploy/` on the VM.
3. Create `~/cloud-cicd-lab/deploy/.env` with `DB_PASSWORD=<strong secret>`; never commit it.
4. Make GHCR image accessible using `docker login ghcr.io` (use a minimally scoped token if the package is private) or make the package public.
5. From `deploy/`: `APP_IMAGE=ghcr.io/OWNER/REPO:sha-<commit> bash deploy.sh`.
6. Install `deploy/nginx.conf` in `/etc/nginx/sites-available/cloud-cicd-lab` and enable it, then `sudo nginx -t && sudo systemctl reload nginx`.
7. Test `curl http://VM_PUBLIC_IP/actuator/health` (HTTP demo only; add a domain + HTTPS before public production use).

Rollback: run `APP_IMAGE=ghcr.io/OWNER/REPO:sha-<previous_commit> bash deploy.sh`.

## API

- `GET /actuator/health`
- `GET /api/tasks`
- `POST /api/tasks` JSON `{"title":"demo"}`
- `PATCH /api/tasks/1` JSON `{"completed":true}`
- `DELETE /api/tasks/1`

## Limits / planned improvements

This is a lab, **not production ready**: the API has no authentication, `ddl-auto=update` should be replaced with migrations, HTTPS is not configured, deployment is manual, and Docker Compose is single-host. Do not expose sensitive data. To add: Flyway, TLS, image vulnerability scan, automatic gated CD, backup/restore and integration tests.
