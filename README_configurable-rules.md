## Drools CloudEvents configurable-rules Operator

This configurable-rules version of Drools CloudEvents Operator. You can change the rules under `config-rules` directory and restart the container to apply the new rules.

### Steps (with running java)

1. [Term 1] `minikube start --mount --mount-string="$(pwd)/config-rules:/mnt/data"`
2. [Term 1] `minikube addons enable ingress`
3. [Term 1] `kubectl apply -f ./config/exposedapps.halkyon.io-v1.crd.yml`
4. [Term 1] `kubectl apply -f ./config/config-rules-pv-pvc.yml`
5. [Term 2] `mvn clean package`
6. [Term 2] `java -jar target/quarkus-app/quarkus-run.jar`
7. [Term 1] `kubectl apply -f ./config/drools-cloudevents-configurable-rules.yml`
8. Wait until [Term 2] prints `App drools-cloudevents-configurable-rules is exposed and ready to be used at https://192.168.x.x` It may take around 15 seconds.
9. [Term 1] 
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
   {"color":"red"}
   ```

### Steps (with container image)

1. `minikube start --mount --mount-string="$(pwd)/config-rules:/mnt/data"`
2. `minikube addons enable ingress`
3. `minikube addons enable olm`
4. `kubectl apply -f drools-cloudevents-configurable-rules-operator-deployment.yaml`
5. Wait until drools-cloudevents-configurable-rules-operator is running.
6. `kubectl apply -f ./config/config-rules-pv-pvc.yml`
7. `kubectl apply -f ./config/drools-cloudevents-configurable-rules.yml`
8. `kubectl logs <drools-cloudevents-configurable-rules-operator pod name> -f`
9. Wait until the log prints `App drools-cloudevents-configurable-rules is exposed and ready to be used at https://192.168.x.x` It may take around 30 seconds.
10.
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
11. You should get a response:
   ```
   {"color":"red"}
   ```

