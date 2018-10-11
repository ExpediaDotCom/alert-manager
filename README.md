[![Build Status](https://travis-ci.org/ExpediaDotCom/alert-manager.svg?branch=master)](https://travis-ci.org/ExpediaDotCom/alert-manager)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/ExpediaDotCom/alert-manager/blob/master/LICENSE)

# Alert Manager (AM)

Manages Alerts and Subscriptions

[Wiki documentation](https://github.com/ExpediaDotCom/alert-manager/wiki)

## Build

To build the Maven project:

```
$ ./mvnw clean verify
```

### How the Travis CI build works

We use Travis CI to build AM Docker images and push them to Docker Hub. Here's how it works:

- A developer pushes a branch (`master` or otherwise) to GitHub.
- GitHub kicks off a Travis CI build.
- Travis CI reads `.travis.yml`, which drives the build.
- `.travis.yml` invokes the `Makefile`.
- The `Makefile` runs a Maven build for the whole project and releases Docker image.
- For the release (docker push), it uses the `docker/publish-to-docker-hub.sh` script. This script has the logic to push the image to Docker Hub
  if and only if the current branch is the `master`.
