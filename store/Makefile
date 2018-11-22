.PHONY: integration_test release

MAVEN := ../mvnw
export DOCKER_ORG := expediadotcom
export DOCKER_IMAGE_NAME := alert-manager-store

docker_build: copy_backend_jars
	docker build -t $(DOCKER_IMAGE_NAME) -f docker/Dockerfile .

copy_backend_jars:
	mkdir -p storage-backends
	cp ../storage-backends/elasticsearch/target/elasticsearch-store-1.0.0-SNAPSHOT.jar storage-backends/elasticsearch-store.jar

integration_test: docker_build
	docker-compose -f docker/docker-compose.yaml up -d
	# sleep for 30 sec to let elastic, kafka to get up
	sleep 30

	# run the integration tests now.
	${MAVEN} test -Pintegration-tests

	docker-compose -f docker/docker-compose.yaml stop

# build all and publish docker images
release: docker_build
	../deployment/scripts/publish-to-docker-hub.sh
