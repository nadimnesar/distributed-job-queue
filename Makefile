MINIKUBE ?= minikube
KUBECTL ?= kubectl
K8S_DIR ?= k8s
NAMESPACE ?= distributed-job-queue

.PHONY: start status ip build namespace apply delete pod svc pvc

start:
	$(MINIKUBE) start

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

apply: namespace
	$(KUBECTL) apply -f $(K8S_DIR)/ --recursive

pod:
	$(KUBECTL) get pods -n $(NAMESPACE)

svc:
	$(KUBECTL) get svc -n $(NAMESPACE)

pvc:
	$(KUBECTL) get pvc -n $(NAMESPACE)

logs:
	$(KUBECTL) logs -n $(NAMESPACE) -l app=rabbitmq --tail=100

delete:
	$(KUBECTL) delete -f $(K8S_DIR)/ --recursive

restart: delete build apply
