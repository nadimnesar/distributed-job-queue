MINIKUBE ?= minikube
KUBECTL ?= kubectl
NAMESPACE ?= distributed-job-queue
OBSERVABILITY_NAMESPACE ?= observability
OVERLAY ?= k8s/overlays/minikube

.PHONY: start status ip build namespace apply render pod svc pvc pod-obs svc-obs pvc-obs logs-rabbitmq logs-fluentbit logs-es logs-kibana delete

start:
	$(MINIKUBE) start
	minikube addons enable ingress

status:
	$(MINIKUBE) status

ip:
	$(MINIKUBE) ip

build:
	@eval $$($(MINIKUBE) docker-env) && \
		docker build -t producer:latest ./producer && \
		docker build -t worker:latest ./worker && \
		docker build -t migration:latest ./migration

namespace:
	@$(KUBECTL) create namespace $(NAMESPACE) --dry-run=client -o yaml | $(KUBECTL) apply -f -
	@$(KUBECTL) create namespace $(OBSERVABILITY_NAMESPACE) --dry-run=client -o yaml | $(KUBECTL) apply -f -

apply:
	$(KUBECTL) apply -k $(OVERLAY)

render:
	$(KUBECTL) kustomize $(OVERLAY)

pod:
	$(KUBECTL) get pods -n $(NAMESPACE)

svc:
	$(KUBECTL) get svc -n $(NAMESPACE)

pvc:
	$(KUBECTL) get pvc -n $(NAMESPACE)

pod-obs:
	$(KUBECTL) get pods -n $(OBSERVABILITY_NAMESPACE)

svc-obs:
	$(KUBECTL) get svc -n $(OBSERVABILITY_NAMESPACE)

pvc-obs:
	$(KUBECTL) get pvc -n $(OBSERVABILITY_NAMESPACE)

logs-rabbitmq:
	$(KUBECTL) logs -n $(NAMESPACE) -l app=rabbitmq --tail=1000 -f

logs-fluentbit:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=fluentbit --tail=1000 -f

logs-es:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=elasticsearch --tail=1000 -f

logs-kibana:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=kibana --tail=1000 -f

delete:
	$(KUBECTL) delete -k $(OVERLAY)
