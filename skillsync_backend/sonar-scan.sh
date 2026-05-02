#!/bin/bash

# =============================================================
#  SkillSync — SonarQube Full Scan Script
#  Run this after SonarQube container is up and token is set.
#
#  Usage:
#    chmod +x sonar-scan.sh
#    ./sonar-scan.sh <YOUR_SONAR_TOKEN>
#
#  Example:
#    ./sonar-scan.sh sqa_abc123def456
# =============================================================

set -e

SONAR_HOST="http://localhost:9000"
SONAR_TOKEN="${1}"

if [ -z "$SONAR_TOKEN" ]; then
  echo "❌  Usage: ./sonar-scan.sh <YOUR_SONAR_TOKEN>"
  echo "    Get your token from: http://localhost:9000 → My Account → Security → Generate Token"
  exit 1
fi

SERVICES=("authservice" "sessionservice" "reviewservice" "skillservice" "groupservice")

echo ""
echo "=============================================="
echo "  SkillSync SonarQube Scan"
echo "  Host  : $SONAR_HOST"
echo "=============================================="
echo ""

for svc in "${SERVICES[@]}"; do
  echo "----------------------------------------------"
  echo "  Scanning: $svc"
  echo "----------------------------------------------"

  cd "$svc"

  # Step 1: Run tests + generate JaCoCo XML report
  echo "  ▶ Running: mvn clean verify"
  mvn clean verify \
    -Dspring.datasource.url="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" \
    -Dspring.datasource.driver-class-name=org.h2.Driver \
    -Dspring.datasource.username=sa \
    -Dspring.datasource.password= \
    -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
    -Deureka.client.enabled=false \
    -Dspring.cloud.compatibility-verifier.enabled=false \
    -Dmaven.test.failure.ignore=true \
    --no-transfer-progress

  # Step 2: Run SonarQube analysis
  echo "  ▶ Running: mvn sonar:sonar"
  mvn sonar:sonar \
    -Dsonar.host.url="$SONAR_HOST" \
    -Dsonar.token="$SONAR_TOKEN" \
    -Dsonar.projectKey="$svc" \
    -Dsonar.projectName="SkillSync - $svc" \
    -Dsonar.coverage.jacoco.xmlReportPaths="target/site/jacoco/jacoco.xml" \
    --no-transfer-progress

  echo "  ✅  $svc scan complete"
  echo ""

  cd ..
done

echo "=============================================="
echo "  ✅  All services scanned!"
echo "  📊  View results at: $SONAR_HOST"
echo "      Login: admin / admin (change on first login)"
echo "=============================================="
