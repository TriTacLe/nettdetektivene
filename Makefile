.PHONY: dev prod down clean logs status test test-backend test-frontend test-e2e test-coverage

# configurable
SERVICES_DEV = mysql backend frontend
SERVICES_PROD = mysql backend frontend

# Dev
dev:
	@echo "Starting backend services ($(SERVICES_DEV))..."
	docker compose up -d --build $(SERVICES_DEV)

	@echo "Waiting for backend (http://localhost:8080)..."
	@retries=30; \
	until curl -s http://localhost:8080 >/dev/null 2>&1; do \
		if [ $$retries -le 0 ]; then \
			echo "Backend did not become ready in time"; \
			echo "Last backend logs:"; \
			docker compose logs --tail=50 backend; \
			exit 1; \
		fi; \
		printf "."; \
		retries=$$((retries-1)); \
		sleep 2; \
	done; \
	echo ""

	@echo "Backend is up"

	@echo "Waiting for frontend (http://localhost:5173)..."
	@retries=30; \
	until curl -s http://localhost:5173 >/dev/null 2>&1; do \
		if [ $$retries -le 0 ]; then \
			echo "Frontend did not become ready in time"; \
			echo "Last frontend logs:"; \
			docker compose logs --tail=50 frontend; \
			exit 1; \
		fi; \
		printf "."; \
		retries=$$((retries-1)); \
		sleep 2; \
	done; \
	echo ""

	@echo "Frontend is up"
	@echo "Frontend: http://localhost:5173"
	@echo "Backend:  http://localhost:8080"
	@echo "MySQL:    localhost:3307"

	@echo "Seeding handled by the backend's DataSeeder (Spring 'seed' profile, set in compose.yaml)."

	@echo "Streaming logs (Ctrl+C to stop)..."
	docker compose logs -f backend frontend mysql


# Prod
prod:
	@echo "Starting full stack ($(SERVICES_PROD))..."
	docker compose up -d --build $(SERVICES_PROD)

	@echo "Checking backend health..."
	@retries=30; \
	until curl -s http://localhost:8080 >/dev/null 2>&1; do \
		if [ $$retries -le 0 ]; then \
			echo "Backend failed to start"; \
			docker compose logs --tail=50 backend; \
			exit 1; \
		fi; \
		printf "."; \
		retries=$$((retries-1)); \
		sleep 2; \
	done; \
	echo ""

	@echo "All services started"
	@echo "Frontend: http://localhost:5173"
	@echo "Backend:  http://localhost:8080"
	@echo "MySQL:    localhost:3307"

	@echo "Showing recent logs:"
	docker compose logs --tail=30

	@echo "Follow logs with: make logs"


# Helpers
status:
	@echo "Ports (8080, 3307, 5173):"
	@sudo lsof -i:8080,3307,5173 -sTCP:LISTEN 2>/dev/null || echo "  none"
	@echo ""
	@echo "Docker containers:"
	@docker ps --format "  {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "  none"

down:
	@echo "Stopping services..."
	docker compose down

clean:
	@echo "Stopping main compose project (containers, volumes, orphans, networks)..."
	docker compose down -v --remove-orphans
	@echo "Stopping alternate backend/ compose project, if any..."
	-docker compose -f backend/compose.yaml down -v --remove-orphans 2>/dev/null
	@echo "Removing locally-built images for this project..."
	-docker image rm -f backend:latest frontend:latest 2>/dev/null
	-docker image ls --format '{{.Repository}}:{{.Tag}}' | grep -E '^(idatt2106-v26-05|backend|nettdetektivene)[-_/].*:' | xargs -r docker image rm -f
	@echo "Removing any stray named volumes for this project (in case of project-name drift)..."
	-docker volume rm -f nettdetektivene_mysql_data nettdetektivene_uploads 2>/dev/null
	-docker volume ls --format '{{.Name}}' | grep -E 'nettdetektivene_(mysql_data|uploads)$$' | xargs -r docker volume rm -f
	@echo "Done."

logs:
	@echo "Tailing logs..."
	docker compose logs -f $(filter-out $@,$(MAKECMDGOALS))

# Testing
test: test-backend test-frontend
	@echo "All tests completed"

test-backend:
	@echo "Running backend tests..."
	cd backend && ./mvnw clean test -q

test-frontend:
	@echo "Running frontend unit tests..."
	cd frontend && npm run test:unit -- --run

test-e2e:
	@echo "Running E2E tests..."
	@echo "Make sure the app is running (make dev) before running E2E tests"
	cd frontend && npx cypress run

test-coverage:
	@echo "Running backend tests with coverage..."
	cd backend && ./mvnw clean verify -q
	@echo "Coverage report: backend/target/site/jacoco/index.html"
