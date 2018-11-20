.PHONY: all release

PWD := $(shell pwd)
MAVEN := ./mvnw

clean:
	${MAVEN} clean

build:
	${MAVEN} install package

all: clean build integration_test

integration_test:
	$(MAKE) -C store integration_test
	$(MAKE) -C service integration_test

# build all and release
release: all
	$(MAKE) -C service release
	$(MAKE) -C store release
	$(MAKE) -C notifier release
	$(MAKE) -C deprecated-alertmanager release
	./.travis/deploy.sh
