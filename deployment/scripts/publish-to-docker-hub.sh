#!/usr/bin/env bash

# Copyright 2018-2019 Expedia Group, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

QUALIFIED_DOCKER_IMAGE_NAME=$DOCKER_ORG/$DOCKER_IMAGE_NAME
echo "DOCKER_ORG=$DOCKER_ORG, DOCKER_IMAGE_NAME=$DOCKER_IMAGE_NAME, QUALIFIED_DOCKER_IMAGE_NAME=$QUALIFIED_DOCKER_IMAGE_NAME"
echo "BRANCH=$BRANCH, TAG=$TAG, SHA=$SHA"

# login
[[ -z $DOCKER_PASSWORD ]] && printf "\nPlease enter your docker.io password\n"
docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

# Add tags
if [[ $TAG =~ ([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "releasing semantic versions"

    unset MAJOR MINOR PATCH
    MAJOR="${BASH_REMATCH[1]}"
    MINOR="${BASH_REMATCH[2]}"
    PATCH="${BASH_REMATCH[3]}"

    # for tag, add MAJOR, MAJOR.MINOR, MAJOR.MINOR.PATCH and latest as tag
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR.$MINOR
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$MAJOR.$MINOR.$PATCH
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:latest

    # publish image with tags
    docker push $QUALIFIED_DOCKER_IMAGE_NAME

elif [[ "$BRANCH" == "master" ]]; then
    echo "releasing master branch"

    # for 'master' branch, add SHA as tags
    docker tag $DOCKER_IMAGE_NAME $QUALIFIED_DOCKER_IMAGE_NAME:$SHA

    # publish image with tags
    docker push $QUALIFIED_DOCKER_IMAGE_NAME
fi
