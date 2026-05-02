# SkillSync — Docker Setup Guide

## Architecture

```
                         ┌─────────────────────────────────────────────────────┐
                         │              Docker Network: skillsync-net          │
                         │                                                     │
  Browser ──► :4000 frontend ──► :8080 apigateway ──► eurekaserver (:8761)    │
                              │                                                │
               ┌──────────────┼──────────────────────────────┐                │
               ▼              ▼              ▼                ▼                │
          authservice    skillservice   sessionservice    groupservice         │
            (:8081)        (:8082)        (:8083)          (:8086)            │
               │                            │                                  │
               │                    notificationservice                        │
               │                        (:8084)                               │
               │                            │                                  │
               └────────────────────────────┼──► reviewservice (:8085)        │
                                            │                                  │
         Infrastructure:                   │                                  │
           postgres (:5432)  ◄─────────────┘                                  │
           rabbitmq (:5672, :15672)                                           │
           redis    (:6379)                                                   │
           zipkin   (:9411)                                                   │
           sonarqube(:9000)                                                   │
         └─────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

- **Docker Desktop** installed and running
  - Mac/Windows: https://www.docker.com/products/docker-desktop
  - Linux: https://docs.docker.com/engine/install/
- At least **6 GB RAM** allocated to Docker (Docker Desktop → Settings → Resources)
- Ports 4000, 8080–8086, 5433, 5672, 6379, 9000, 9411, 15672 must be free

---

## Step 1 — Clone / Extract the Project

Place the extracted `skillsync_modified` folder somewhere on your machine, e.g.:
```
C:\Projects\skillsync_modified\   (Windows)
~/Projects/skillsync_modified/    (Mac/Linux)
```

---

## Step 2 — (Linux/WSL only) Fix vm.max_map_count for SonarQube

```bash
sudo sysctl -w vm.max_map_count=524288
sudo sysctl -w fs.file-max=131072
```

Mac and Windows Docker Desktop users can skip this.

---

## Step 3 — Start Everything

Open a terminal inside the `skillsync_modified` folder and run:

```bash
docker-compose up --build
```

**First run takes 5–10 minutes** — Docker builds all 8 service images and downloads dependencies.

To run in the background (detached mode):

```bash
docker-compose up --build -d
```

---

## Step 4 — Wait for Services to Start

Watch the logs. Services start in this order automatically:

| Order | Service | Ready when you see |
|-------|---------|-------------------|
| 1 | postgres | `database system is ready to accept connections` |
| 2 | rabbitmq | `Server startup complete` |
| 3 | redis | `Ready to accept connections` |
| 4 | eurekaserver | `Started EurekaserverApplication` |
| 5 | All microservices | `Started ...Application in X seconds` |
| 6 | apigateway | `Started ApigatewayApplication` |
| 7 | frontend | `Node Express server listening on http://localhost:4000` |

Full startup takes about **90–120 seconds** after images are built.

---

## Step 5 — Access the Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Frontend app** | http://localhost:4000 | — |
| **Swagger UI** (all APIs) | http://localhost:8080/swagger-ui.html | — |
| **Eureka Dashboard** | http://localhost:8761 | — |
| **RabbitMQ Dashboard** | http://localhost:15672 | guest / guest |
| **Zipkin Tracing** | http://localhost:9411 | — |
| **SonarQube** | http://localhost:9000 | admin / admin |
| authservice direct | http://localhost:8081/swagger-ui.html | — |
| skillservice direct | http://localhost:8082/swagger-ui.html | — |
| sessionservice direct | http://localhost:8083/swagger-ui.html | — |
| notificationservice | http://localhost:8084 | — |
| reviewservice direct | http://localhost:8085/swagger-ui.html | — |
| groupservice direct | http://localhost:8086/swagger-ui.html | — |

---

## Step 6 — Test the API via Swagger

1. Open http://localhost:8080/swagger-ui.html
2. Select **Auth Service** from the dropdown
3. Call `POST /auth/login`:
   ```json
   { "email": "admin@gmail.com", "password": "admin123" }
   ```
4. Copy the JWT token from the response
5. Click **Authorize 🔒** at the top → paste the token → click Authorize
6. All protected endpoints now work!

> A default admin is auto-created on first startup: `admin@gmail.com` / `admin123`

---

## Step 7 — Run SonarQube Scan (Optional)

After SonarQube is up at http://localhost:9000:

```bash
# 1. Login at http://localhost:9000 → My Account → Security → Generate Token
# 2. Run the scan script from project root:
chmod +x sonar-scan.sh
./sonar-scan.sh YOUR_TOKEN_HERE
```

See `README_SONAR_SETUP.md` for full details.

---

## Common Commands

```bash
# Start all (background)
docker-compose up -d

# Start only infra (postgres, rabbitmq, redis, zipkin)
docker-compose up -d postgres rabbitmq redis zipkin

# Stop all services (keeps data volumes)
docker-compose down

# Stop and delete ALL data (fresh start)
docker-compose down -v

# View logs for a specific service
docker-compose logs -f authservice
docker-compose logs -f apigateway
docker-compose logs -f frontend

# Rebuild a single service after code change
docker-compose up --build authservice
docker-compose up --build frontend

# Check what's running
docker-compose ps

# Open a shell inside a container
docker exec -it skillsync-auth bash
docker exec -it skillsync-postgres psql -U postgres
```

---

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| Service crashes immediately | DB not ready yet | Docker healthchecks handle this, wait 30s and retry |
| `vm.max_map_count` error | SonarQube needs higher limit | `sudo sysctl -w vm.max_map_count=524288` |
| Port already in use | Another app on that port | Stop the conflicting app or change port in `docker-compose.yml` |
| OOM / containers killed | Not enough RAM | Allocate ≥6GB in Docker Desktop → Settings → Resources |
| Cannot reach Eureka | Eureka not yet started | Wait — services will retry automatically with `restart: unless-stopped` |
| Email not sending | Wrong Gmail credentials | Update `MAIL_USERNAME` and `MAIL_PASSWORD` in `.env` with an App Password |
| SonarQube unreachable | Still starting | Wait 60s after `docker-compose up`, check `docker logs skillsync-sonarqube` |
| Build fails — mvnw not executable | Line endings on Windows | Run: `git config --global core.autocrlf false` and re-extract the zip |
