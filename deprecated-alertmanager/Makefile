.PHONY: integration_test release

MAVEN := ../mvnw

export DOCKER_ORG := expediadotcom
export DOCKER_IMAGE_NAME := alert-manager-deprecated

integration_test:
	${MAVEN} test

docker_build:
	docker build -t $(DOCKER_IMAGE_NAME) -f docker/Dockerfile .

# build all and publish docker images
release: docker_build
	../deployment/scripts/publish-to-docker-hub.sh
