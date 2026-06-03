apply:
	kubectl apply -f k8s/ --recursive

delete:
	kubectl delete -f k8s/ --recursive

pods:
	kubectl get pods -n distributed-job-queue

services:
	kubectl get svc -n distributed-job-queue

ip:
	minikube ip
