## Exposed App Operator

This project is copied from https://github.com/quarkiverse/quarkus-operator-sdk/tree/main/samples/exposedapp .

At the moment, this project is a simple operator that exposes an existing containerized application as a Kubernetes Custom Resource Definition (CRD).

So we can expose 'tkobayas/drools-cloudevents:latest' in a Kubernetes cluster using `src/main/resources/drools.yml`.

### Steps (with minikube)

1. [Term 1] `minikube start`
2. [Term 1] `minikube addons enable ingress`
3. [Term 2] `mvn clean package`
4. [Term 1] `kubectl apply -f ./target/bundle/drools-cloudevents-operator/manifests/exposedapps.halkyon.io-v1.crd.yml`
5. [Term 2] `java -jar target/quarkus-app/quarkus-run.jar`
6. [Term 1] kubectl apply -f ./src/main/resources/drools.yml
7. Wait until [Term 2] prints `App drools-cloudevents is exposed and ready to be used at https://192.168.x.x`
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
   Note that you need to use the IP address in the step 7. Also use `-k` option to ignore the self-signed certificate.
9. You should get a response:
   ```
   ...
   {"color":"red"}
   ```
   
In this example, 'tkobayas/drools-cloudevents:latest' is a container image with a static rule. Then next step is to

A. Use a container image which accepts dynamic rules. The application compiles the rules at runtime.

B. Accept dynamic rules from CR creation. Then build and push a new container image so that pods can use the new image. This would require more effort, but it has "fast start-up" advantage.
