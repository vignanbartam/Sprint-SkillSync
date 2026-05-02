# SkillSync — SonarQube Setup Guide

## What Was Added

| Component | Detail |
|-----------|--------|
| **SonarQube 10.4** | Runs in Docker on `http://localhost:9000` |
| **JaCoCo 0.8.11** | Generates XML coverage reports per service |
| **H2 Database** | In-memory DB so tests run without PostgreSQL |
| **80% coverage rule** | JaCoCo `check` goal enforces minimum 80% instruction coverage |
| **Sonar exclusions** | Entities, DTOs, config, Application.java excluded (not business logic) |

---

## Prerequisites

Make sure the following are installed on your machine:

- **Docker Desktop** (running) — https://www.docker.com/products/docker-desktop
- **Java 17**
- **Maven 3.8+**

---

## Step 1 — Start SonarQube via Docker

Open a terminal in the project root (where `docker-compose.sonar.yml` is) and run:

```bash
docker-compose -f docker-compose.sonar.yml up -d
```

Wait about 60 seconds for SonarQube to fully start. Then open:

```
http://localhost:9000
```

Login with:
- **Username:** `admin`
- **Password:** `admin`

> SonarQube will ask you to change your password on first login. Set it to something you remember (e.g. `Admin@123`).

---

## Step 2 — Generate a SonarQube Token

1. After logging in, click your **profile icon** (top right)
2. Go to **My Account → Security**
3. Under **Generate Tokens**, enter name: `skillsync-token`
4. Click **Generate**
5. **Copy the token immediately** — it will not be shown again

Example token format: `sqa_abc123def456xyz789`

---

## Step 3 — Fix vm.max_map_count (Linux/WSL only)

If SonarQube fails to start on Linux or WSL, run this once:

```bash
sudo sysctl -w vm.max_map_count=524288
sudo sysctl -w fs.file-max=131072
```

Mac and Windows Docker Desktop users can skip this step.

---

## Step 4 — Run the Scan Script

Make the script executable, then run it with your token:

```bash
chmod +x sonar-scan.sh
./sonar-scan.sh sqa_abc123def456xyz789
```

This will, for **each of the 5 services**:
1. Run `mvn clean verify` — compiles, runs all JUnit tests, generates JaCoCo XML coverage report
2. Run `mvn sonar:sonar` — uploads results to SonarQube

> The full scan takes roughly **3–5 minutes** on first run (Maven downloads dependencies). Subsequent runs are faster.

---

## Step 5 — View Results in SonarQube

Go to `http://localhost:9000` → **Projects**

You will see 5 projects:

| Project | Expected Coverage |
|---------|------------------|
| SkillSync - authservice | ≥ 80% |
| SkillSync - sessionservice | ≥ 80% |
| SkillSync - reviewservice | ≥ 80% |
| SkillSync - skillservice | ≥ 80% |
| SkillSync - groupservice | ≥ 80% |

Click any project to see:
- **Coverage %** (line and instruction coverage)
- **Code Smells**
- **Bugs**
- **Vulnerabilities**
- **Duplications**

---

## Step 6 — Run Tests Only (Without Sonar)

To just run tests and see the JaCoCo HTML coverage report locally:

```bash
cd authservice
mvn clean verify
# Open: authservice/target/site/jacoco/index.html in your browser
```

Repeat for each service.

---

## What Each Service's Tests Cover

### authservice
| Test Class | Covers |
|-----------|--------|
| `AuthServiceTest` | register, login — success + 3 failure cases |
| `AuthControllerTest` | POST /register, POST /login, GET /user/{id} |
| `AdminControllerTest` | PUT /admin/mentors/{id}/approve — admin + forbidden |
| `MentorControllerTest` | POST /user/mentor-application |
| `MentorApplicationServiceTest` | apply, approve — success + admin block + not found |
| `GlobalExceptionHandlerTest` | All 5 exception types + response structure |
| `JwtUtilTest` | generateToken, extractEmail, extractRole, invalid token |

### sessionservice
| Test Class | Covers |
|-----------|--------|
| `SessionServiceTest` | book, updateStatus — 6 cases |
| `SessionControllerTest` | All 5 endpoints |
| `GlobalExceptionHandlerTest` | All exception types |
| `JwtUtilTest` | validate, getRole, invalid token |

### reviewservice
| Test Class | Covers |
|-----------|--------|
| `ReviewServiceTest` | addReview, getReviews — 6 cases |
| `ReviewControllerTest` | POST /reviews, GET /reviews/mentor/{id} |
| `GlobalExceptionHandlerTest` | All exception types |
| `JwtUtilTest` | generateToken, extractEmail, extractRole |

### skillservice
| Test Class | Covers |
|-----------|--------|
| `SkillServiceTest` | addSkill, getAllSkills, deleteSkill — 8 cases |
| `SkillControllerTest` | GET /skills |
| `AdminSkillControllerTest` | POST /admin/skills, DELETE /admin/skills/{id} |
| `GlobalExceptionHandlerTest` | All exception types |
| `JwtUtilTest` | validateToken, extractEmail, extractRole |

### groupservice
| Test Class | Covers |
|-----------|--------|
| `GroupServiceTest` | create, getAllGroups, requestJoin, approve, remove — 10 cases |
| `GroupControllerTest` | All 5 endpoints |
| `GlobalExceptionHandlerTest` | All exception types |
| `UserContextTest` | getUserId — success, missing header, invalid value, blank |

---

## Stopping SonarQube

```bash
docker-compose -f docker-compose.sonar.yml down
```

To also delete all SonarQube data (start fresh):

```bash
docker-compose -f docker-compose.sonar.yml down -v
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| SonarQube not starting | Wait 60s. Check logs: `docker logs sonarqube` |
| `vm.max_map_count` error | Run: `sudo sysctl -w vm.max_map_count=524288` |
| Build fails — DB connection | Tests use H2 in-memory, no PostgreSQL needed |
| Token invalid | Regenerate at `http://localhost:9000` → My Account → Security |
| Coverage below 80% | Build will fail — check `target/site/jacoco/index.html` to see which lines are uncovered |
| Port 9000 in use | Change port in `docker-compose.sonar.yml`: `"9001:9000"` |
