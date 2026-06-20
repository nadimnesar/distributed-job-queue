MINIKUBE ?= minikube
KUBECTL ?= kubectl
NAMESPACE ?= distributed-job-queue
OBSERVABILITY_NAMESPACE ?= observability
OVERLAY ?= k8s/overlays/minikube

.PHONY: start status ip build apply render pod svc pvc logs-postgres logs-rabbitmq logs-fluentbit logs-es logs-kibana delete

start:
	$(MINIKUBE) start
	minikube addons enable ingress

status:
	$(MINIKUBE) status

ip:
	$(MINIKUBE) ip

build:
	@eval $$($(MINIKUBE) docker-env) && \
		docker build --build-arg MODULE=producer -t producer:latest . && \
		docker build --build-arg MODULE=worker -t worker:latest . && \
		docker build --build-arg MODULE=migration -t migration:latest .

apply:
	$(KUBECTL) apply -k $(OVERLAY)

render:
	$(KUBECTL) kustomize $(OVERLAY)

pod:
	$(KUBECTL) get pods -n $(NAMESPACE)
	$(KUBECTL) get pods -n $(OBSERVABILITY_NAMESPACE)

svc:
	$(KUBECTL) get svc -n $(NAMESPACE)
	$(KUBECTL) get svc -n $(OBSERVABILITY_NAMESPACE)

pvc:
	$(KUBECTL) get pvc -n $(NAMESPACE)
	$(KUBECTL) get pvc -n $(OBSERVABILITY_NAMESPACE)

logs-postgres:
	$(KUBECTL) logs -n $(NAMESPACE) -l app=postgres --tail=1000 -f

logs-rabbitmq:
	$(KUBECTL) logs -n $(NAMESPACE) -l app=rabbitmq --tail=1000 -f

logs-fluentbit:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=fluentbit --tail=1000 -f

logs-es:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=elasticsearch --tail=1000 -f

logs-kibana:
	$(KUBECTL) logs -n $(OBSERVABILITY_NAMESPACE) -l app=kibana --tail=1000 -f

delete:
	$(KUBECTL) delete -k $(OVERLAY) --ignore-not-found
	$(KUBECTL) delete pvc --all -n $(NAMESPACE) --ignore-not-found
	$(KUBECTL) delete pvc --all -n $(OBSERVABILITY_NAMESPACE) --ignore-not-found
