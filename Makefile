# Fever Plans API - Makefile

.PHONY: help build test run clean

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Development
run: ## Start the application
	./gradlew bootRun

dev: clean build run ## Clean, build and run

setup: ## Setup development environment
	@chmod +x ./gradlew
	@mkdir -p ./data

# Build
build: ## Build the application
	./gradlew clean build

build-fast: ## Build without tests
	./gradlew clean assemble

jar: ## Create executable JAR
	./gradlew bootJar

clean: ## Clean build artifacts
	./gradlew clean
	@rm -rf ./data/*.mv.db ./data/*.trace.db
# Testing
test: ## Run all tests
	./gradlew test

# API Testing
test-api: ## Test search endpoint - Need server running
	@curl -s "http://localhost:8080/search?starts_at=2021-02-01&ends_at=2022-07-03" | head -20

# Utilities
db-console: ## Open H2 console info
	@echo "H2 Console: http://localhost:8080/h2-console"
	@echo "JDBC URL: jdbc:h2:file:./data/plansdb"
	@echo "Username: sa"
	@echo "Password: (empty)"

swagger: ## API documentation info
	@echo "API Docs: http://localhost:8080/swagger-ui.html"
