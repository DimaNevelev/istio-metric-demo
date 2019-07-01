# Quick Start

Download istio release:  https://istio.io/docs/setup/kubernetes/#downloading-the-release 

Start istio on minikube
```
minikube delete
minikube start --memory=16384 --cpus=4 --kubernetes-version=v1.14.2
```

Open another terminal and run:
```
minikube tunnel --cleanup
minikube tunnel 
```

Go back to the first terminal:
```
cd istio-X.X.X
export PATH=$PWD/bin:$PATH
for i in install/kubernetes/helm/istio-init/files/crd*yaml; do kubectl apply -f $i; done
kubectl apply -f install/kubernetes/istio-demo.yaml
```

Wait for istio loading. 
Run `watch kubectl get all -A` until all the pods in istio-system namespace are “Running” and “Completed"

```
kubectl label namespace default istio-injection=enabled
```

Clone and build the demo app
```
git clone https://github.com/DimaNevelev/istio-metric-demo.git
cd istio-metric-demo
mvn clean package
eval $(minikube docker-env)
docker build -t istioexample/demo:0.1 .
```

Deploy the metric:
```
kubectl apply -f metric.yaml
```

Deploy the demo app:
```
kubectl apply -f demo.yaml
export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
export SECURE_INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].port}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

Apply prometheus  port-forwarding
```
kubectl -n istio-system port-forward $(kubectl -n istio-system get pod -l app=prometheus -o jsonpath='{.items[0].metadata.name}') 9090:9090 &
```

Init the metrics:
```
curl -s http://${GATEWAY_URL}/api/v1/local
curl -s http://${GATEWAY_URL}/api/v1/internal
curl -s http://${GATEWAY_URL}/api/v1/external
```

Browse to the following url: <http://localhost:9090/graph?g0.range_input=1h&g0.expr=istio_my_request_count%7Bdestination!~%22istio.*%22%7D&g0.tab=1>