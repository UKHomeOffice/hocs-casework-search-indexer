#!/usr/bin/env bash

helm upgrade ${CHART_NAME} \
 ./helm/${CHART_NAME} \
--install \
--reset-values \
--timeout 10m \
--history-max 3 \
--namespace ${KUBE_NAMESPACE} \
--set version=${VERSION} $*
