kubectl create -f configmap.yml -n collector
kubectl create -f secret.yml -n collector
kubectl create -f service.yml -n collector
kubectl create -f deployment.yml -n collector
