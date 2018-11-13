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
	cd api && $(MAKE) release && cd ..
	cd deprecated-alertmanager && $(MAKE) release && cd ..
	cd notifier && $(MAKE) release && cd ..
