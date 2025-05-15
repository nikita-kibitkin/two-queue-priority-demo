[![Java CI with Maven](https://github.com/echovarga/two-queue-priority-demo/actions/workflows/maven.yml/badge.svg)](https://github.com/echovarga/two-queue-priority-demo/actions/workflows/maven.yml)

# Two-Queue Priority Demo (Spring Boot + Kafka)

## Prerequisites
* Java 17
* Maven 3.9+
* Docker / Docker Compose v2

## Quick Start
```bash
git clone https://github.com/echovarga/two-queue-priority-demo.git
cd two-queue-priority-demo
mvn clean package -DskipTests
# build JAR goes to docker build
docker compose up --build -d
```
Available in a couple of minutes:
* **Prometheus:** <http://localhost:9090>
* **Grafana:** <http://localhost:3000> (login admin/admin)
*  `grafana/priority_dashboard.json` → dashboard with P50/P95/P99.

## Configuration
application.yaml contains `number-of-messages`, `high-queue-chance`(0.5=50% chance. 0=normal queue only), `lambda` (messages per second) parameters.  Change and run `docker compose up --build -d`.
