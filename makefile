DOCKER_COMPOSE_RUN_COMMAND = docker compose up --build -d

dev:
	$(MAKE) -C backend build-fast
	$(DOCKER_COMPOSE_RUN_COMMAND)

prod:
	$(MAKE) -C backend build-prod
	$(DOCKER_COMPOSE_RUN_COMMAND)