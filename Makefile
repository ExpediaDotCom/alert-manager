.PHONY: all release

PWD := $(shell pwd)
MAVEN := ./mvnw

clean:
	${MAVEN} clean

build:
	${MAVEN} install package

all: clean build

# build all and release
release: all
	$(MAKE) -C service release
	$(MAKE) -C store release
	$(MAKE) -C notifier release
	$(MAKE) -C deprecated-alertmanager release
