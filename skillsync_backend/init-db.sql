-- =============================================================
--  SkillSync — Database Initialisation
--  Runs automatically on first startup (when data volume is empty).
--  postgres:15 Docker image natively executes .sql files in
--  docker-entrypoint-initdb.d/ — no bash script needed.
--
--  skillsync_auth is created by POSTGRES_DB env var.
--  This file creates the remaining 5 databases.
-- =============================================================

SELECT 'Creating skillsync_skill...' AS status;
CREATE DATABASE skillsync_skill;

SELECT 'Creating skillsync_session...' AS status;
CREATE DATABASE skillsync_session;

SELECT 'Creating skillsync_notification...' AS status;
CREATE DATABASE skillsync_notification;

SELECT 'Creating skillsync_review...' AS status;
CREATE DATABASE skillsync_review;

SELECT 'Creating skillsync_group...' AS status;
CREATE DATABASE skillsync_group;

SELECT 'All SkillSync databases created.' AS status;
