MINIKUBE ?= minikube
KUBECTL ?= kubectl
K8S_DIR ?= k8s
NAMESPACE ?= distributed-job-queue

.PHONY: start status ip namespace apply delete pod svc delete-pvc delete-rabbitmq-pvc delete-postgres-pvc

start:
	$(MINIKUBE) start

status:
	$(MINIKUBE) status

ip:
	$(MINIKUBE) ip

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

delete:
	$(KUBECTL) delete -f $(K8S_DIR)/ --recursive
