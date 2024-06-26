## Drools CloudEvents Operator

This project is copied from https://github.com/quarkiverse/quarkus-operator-sdk/tree/main/samples/exposedapp .

At the moment, this project is a simple operator that exposes an existing containerized application as a Kubernetes Custom Resource Definition (CRD).

So we can expose 'tkobayas/drools-cloudevents:latest' in a Kubernetes cluster using `config/drools-cloudevents.yml`.

Note that this is a very naive configuration and implementation, targeting `default` namespace.

### Steps (with running java)

1. [Term 1] `minikube start`
2. [Term 1] `minikube addons enable ingress`
3. [Term 2] `mvn clean package`
4. [Term 1] `kubectl apply -f ./config/exposedapps.halkyon.io-v1.crd.yml`
5. [Term 2] `java -jar target/quarkus-app/quarkus-run.jar`
6. [Term 1] `kubectl apply -f ./config/drools-cloudevents.yml`
7. Wait until [Term 2] prints `App drools-cloudevents is exposed and ready to be used at https://192.168.x.x` It may take around 15 seconds.
8. [Term 1] 
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
9. You should get a response:
   ```
   {"color":"red"}
   ```

### Steps (with container image)

1. `minikube start`
2. `minikube addons enable ingress`
3. `kubectl apply -f drools-cloudevents-operator-deployment.yaml`
4. Wait until drools-cloudevents-operator is Running (`kubectl get pod -w`).
5. `kubectl apply -f ./config/drools-cloudevents.yml`
6. `kubectl logs <drools-cloudevents-operator pod name> -f`
7. Wait until the log prints `App drools-cloudevents is exposed and ready to be used at https://192.168.x.x` It may take around 30 seconds.
8.
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
9. You should get a response:
   ```
   {"color":"red"}
   ```
10. You can scale the pods by `kubectl scale deployment drools-cloudevents --replicas=3`

In this example, 'tkobayas/drools-cloudevents:latest' is a container image with a static rule. Then next step is to

A. Use a container image which accepts dynamic rules. The application compiles the rules at runtime.

B. Accept dynamic rules from CR creation. Then build and push a new container image so that pods can use the new image. This would require more effort, but it has "fast start-up" advantage.

