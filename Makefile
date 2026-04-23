.PHONY: dev prod down clean logs

# configurable
SERVICES_DEV = mysql backend
SERVICES_PROD = mysql backend frontend

# Dev
dev:
	@echo "▶ Starting backend services ($(SERVICES_DEV))..."
	docker compose up -d --build $(SERVICES_DEV)

	@echo "▶ Waiting for backend (http://localhost:8080)..."
	@retries=30; \
	until curl -s http://localhost:8080 >/dev/null 2>&1; do \
		if [ $$retries -le 0 ]; then \
			echo "✘ Backend did not become ready in time"; \
			echo "▶ Last backend logs:"; \
			docker compose logs --tail=50 backend; \
			exit 1; \
		fi; \
		printf "."; \
		retries=$$((retries-1)); \
		sleep 2; \
	done; \
	echo ""

	@echo "✔ Backend is up"
	@echo "Backend: http://localhost:8080"
	@echo "MySQL:   localhost:3306"

	@echo "▶ Running test-data scripts..."
	@if [ -d scripts/testdata ] && [ "$$(ls -A scripts/testdata 2>/dev/null)" ]; then \
		for f in scripts/testdata/*; do \
			echo "  → Running $$f"; \
			bash "$$f" || { echo "✘ Failed: $$f"; exit 1; }; \
		done; \
		echo "✔ Test data loaded"; \
	else \
		echo "⚠ No scripts found – skipping"; \
	fi

	@echo "▶ Streaming logs (Ctrl+C to stop)..."
	docker compose logs -f backend mysql


# Prod
prod:
	@echo "▶ Starting full stack ($(SERVICES_PROD))..."
	docker compose up -d --build $(SERVICES_PROD)

	@echo "▶ Checking backend health..."
	@retries=30; \
	until curl -s http://localhost:8080 >/dev/null 2>&1; do \
		if [ $$retries -le 0 ]; then \
			echo "✘ Backend failed to start"; \
			docker compose logs --tail=50 backend; \
			exit 1; \
		fi; \
		printf "."; \
		retries=$$((retries-1)); \
		sleep 2; \
	done; \
	echo ""

	@echo "✔ All services started"
	@echo "Frontend: http://localhost:5173"
	@echo "Backend:  http://localhost:8080"
	@echo "MySQL:    localhost:3306"

	@echo "▶ Showing recent logs:"
	docker compose logs --tail=30

	@echo "▶ Follow logs with: make logs"


# Helpers
down:
	@echo "▶ Stopping services..."
	docker compose down

clean:
	@echo "▶ Removing containers + volumes..."
	docker compose down -v

logs:
	@echo "▶ Tailing logs..."
	docker compose logs -f $(filter-out $@,$(MAKECMDGOALS))