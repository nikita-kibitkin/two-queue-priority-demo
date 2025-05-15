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

# build the Spring Boot fat JAR 
mvn clean package -DskipTests

# run the full stack: Zookeeper, Kafka, Prometheus, Grafana, the app
docker compose up --build -d

```
Available in a couple of minutes:
* **Prometheus:** <http://localhost:9090>
* **Grafana:** <http://localhost:3000> (login admin/admin)
* latency_dashboard are available in Grafana and queue.latency metrics are available in Prometheus → see P50/P95/P99.






