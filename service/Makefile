.PHONY: integration_test release

MAVEN := ../mvnw

export DOCKER_ORG := expediadotcom
export DOCKER_IMAGE_NAME := alert-manager-service

docker_build:
	docker build -t $(DOCKER_IMAGE_NAME) -f docker/Dockerfile .

integration_test:
	${MAVEN} test

# build jar, docker image and run integration tests
all: docker_build integration_test

# build all and publish docker images
release: docker_build
	../deployment/scripts/publish-to-docker-hub.sh
