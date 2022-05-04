.PHONY: default auth build push clean
.DEFAULT_GOAL := default

NAME := apx-sdk
REGISTRY := 088921318242.dkr.ecr.us-west-2.amazonaws.com
NAMESPACE := apixio
ECR_LOGIN := "eval $$\( aws ecr get-login --no-include-email \)"

default: auth build push clean

auth:
	@eval $(ECR_LOGIN)

build:
	docker build --no-cache --squash --rm -t ${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION} --build-arg ARTIFACT_USER=${ARTIFACT_USER}  --build-arg ARTIFACT_PASS=${ARTIFACT_PASS} -f Dockerfile ..

push:
	docker tag ${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION} ${REGISTRY}/${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION}
	docker push ${REGISTRY}/${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION}

clean:
	docker rmi -f ${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION} ${REGISTRY}/${NAMESPACE}/${REPOSITORY}/${NAME}:${VERSION}
