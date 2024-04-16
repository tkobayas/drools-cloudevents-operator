## Drools CloudEvents Operator

### Build&Push version

This project is based on https://github.com/quarkiverse/quarkus-operator-sdk/tree/main/samples/exposedapp .

### Steps (with container image)

1. `minikube start --mount --mount-string="$(pwd)/config-rules:/mnt/data"`
2. `minikube addons enable ingress`
3. `minikube addons enable registry`
4. `kubectl apply -f drools-cloudevents-operator-deployment.yaml`
5. Wait until drools-cloudevents-operator is Running (`kubectl get pod -w`).
6. `kubectl get service -n kube-system`
7. Confirm the ClusterIP of the registry service. Write the IP address to `imageRef`'s IP part of config/drools-cloudevents.yml. (TODO: Solve DNS issue)
6. `kubectl apply -f ./config/drools-cloudevents.yml`
7. `kubectl logs <drools-cloudevents-operator pod name> -f`
8. Operator builds drools-cloudevents image using rules mounted via `$(pwd)/config-rules`, pushes it to minikube registry, then, starts a pod with the new image. Wait until the log prints `App drools-cloudevents is exposed and ready to be used at https://192.168.x.x` It may take around 1 minute.
9. 
   ```
   curl -k https://192.168.x.x/drools/evaluate \
   -H "Ce-Specversion: 1.0" \
   -H "Ce-Type: fact.Measurement" \
   -H "Ce-Source: io.cloudevents.examples/user" \
   -H "Ce-Id: 536808d3-88be-4077-9d7a-a3f162705f78" \
   -H "Content-Type: application/json" \
   -H "Ce-Subject: SUBJ-0001" \
   -d '{"id":"color","val":"red"}'
   ```
   Note that you need to replace the IP address with the one in the previous step. Also use `-k` option to ignore the self-signed certificate.
10. You should get a response:
    ```
    {"rebuilt rule result":"red$$"}
    ```

* To build and push THIS operator image, `mvn clean package -DskipTests -Dquarkus.container-image.push=true`
* To scale the pods by `kubectl scale deployment drools-cloudevents --replicas=3`
* To rollout the operator deployment by `kubectl rollout status deployment.apps/drools-cloudevents-operator` when you update the operator deployment.
* To view ClusterIP of register, `kubectl get svc -n kube-system`
