#!/bin/bash
# =============================================================
#  Auto-creates all SkillSync databases inside the postgres container.
#  Runs automatically on first startup via docker-entrypoint-initdb.d/
#  Safe to re-run — skips databases that already exist.
# =============================================================

set -e

create_db() {
  local db=$1
  echo "  → Ensuring database exists: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" \
    -tc "SELECT 1 FROM pg_database WHERE datname = '$db'" \
    | grep -q 1 \
    || psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" \
       -c "CREATE DATABASE \"$db\";"
}

create_db skillsync_auth
create_db skillsync_skill
create_db skillsync_session
create_db skillsync_notification
create_db skillsync_review
create_db skillsync_group

echo "✅ All SkillSync databases are ready."
