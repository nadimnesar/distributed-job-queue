MINIKUBE ?= minikube
KUBECTL ?= kubectl
K8S_DIR ?= k8s
NAMESPACE ?= distributed-job-queue
OBSERVABILITY_NAMESPACE ?= observability

.PHONY: start status ip build namespace apply delete pod svc pvc pod-obs svc-obs pvc-obs logs

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

apply: namespace
	$(KUBECTL) apply -f $(K8S_DIR)/ --recursive

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

logs:
	$(KUBECTL) logs -n $(NAMESPACE) -l app=rabbitmq --tail=1000

delete:
	$(KUBECTL) delete -f $(K8S_DIR)/ --recursive
