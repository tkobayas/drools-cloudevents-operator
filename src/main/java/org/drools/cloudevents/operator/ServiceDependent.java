package org.drools.cloudevents.operator;

import static org.drools.cloudevents.operator.ExposedAppReconciler.LABELS_CONTEXT_KEY;
import static org.drools.cloudevents.operator.ExposedAppReconciler.createMetadata;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDependent extends CRUDKubernetesDependentResource<Service, ExposedApp> {

    static final Logger log = LoggerFactory.getLogger(ServiceDependent.class);

    public ServiceDependent() {
        super(Service.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Service desired(ExposedApp exposedApp, Context context) {

        log.info("ServiceDependent.desired {}", exposedApp.getMetadata().getName());

        final var labels = (Map<String, String>) context.managedDependentResourceContext()
                .getMandatory(LABELS_CONTEXT_KEY, Map.class);

        return new ServiceBuilder()
                .withMetadata(createMetadata(exposedApp, labels))
                .withNewSpec()
                .addNewPort()
                .withName("http")
                .withPort(8080)
                .withNewTargetPort().withValue(8080).endTargetPort()
                .endPort()
                .withSelector(labels)
                .withType("ClusterIP")
                .endSpec()
                .build();
    }
}
