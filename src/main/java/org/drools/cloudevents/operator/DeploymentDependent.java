package org.drools.cloudevents.operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.cloudevents.operator.ExposedAppReconciler.LABELS_CONTEXT_KEY;
import static org.drools.cloudevents.operator.ExposedAppReconciler.createMetadata;

public class DeploymentDependent extends CRUDKubernetesDependentResource<Deployment, ExposedApp>
        implements Matcher<Deployment, ExposedApp> {

    static final Logger log = LoggerFactory.getLogger(DeploymentDependent.class);

    public static volatile MvnState mvnState = MvnState.NOT_EXECUTED;

    public enum MvnState {
        NOT_EXECUTED,
        EXECUTING,
        EXECUTED
    }

    public DeploymentDependent() {
        super(Deployment.class);
    }

    @SuppressWarnings("unchecked")
    public Deployment desired(ExposedApp exposedApp, Context context) {

        String name = exposedApp.getMetadata().getName();
        log.info("DeploymentDependent.desired {}", name);

        synchronized (DeploymentDependent.class) {
            if (mvnState == MvnState.EXECUTING) {
                log.warn("Maven build is still executing for {}", name); // should not be reached if synchronized
            }
            if (mvnState == MvnState.NOT_EXECUTED) {
                buildContainerImage(exposedApp, context);
            }
        }

        final var labels = (Map<String, String>) context.managedDependentResourceContext()
                .getMandatory(LABELS_CONTEXT_KEY, Map.class);

        final var spec = exposedApp.getSpec();
        final var imageRef = spec.getImageRef();
        final var env = spec.getEnv();

        var containerBuilder = new DeploymentBuilder()
                .withMetadata(createMetadata(exposedApp, labels))
                .withNewSpec()
                .withNewSelector().withMatchLabels(labels).endSelector()
                .withNewTemplate()
                .withNewMetadata().withLabels(labels).endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(name).withImage(imageRef);

        // add env variables
        if (env != null) {
            env.forEach((key, value) -> containerBuilder.addNewEnv()
                    .withName(key.toUpperCase())
                    .withValue(value)
                    .endEnv());
        }

        return containerBuilder
                .addNewPort()
                .withName("http").withProtocol("TCP").withContainerPort(8080)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private void buildContainerImage(ExposedApp exposedApp, Context<ExposedApp> context) {
        log.info("Building container image for {}", exposedApp.getMetadata().getName());
        mvnState = MvnState.EXECUTING;

        // Replace files under /opt/build/drools-cloudevents/src/main/resources/ with files under /opt/rules
        Path sourceDirectory = Paths.get("/opt/rules");
        Path destinationDirectory = Paths.get("/opt/build/drools-cloudevents/src/main/resources");
        try {
            copyDirectory(sourceDirectory, destinationDirectory);
        } catch (IOException ex) {
            log.error("Failed to copy configurable rules", ex);
        }

        // build the container image and push it to the registry by running mvn process
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", "mvn clean package -DskipTests");
        builder.directory(new File("/opt/build/drools-cloudevents"));
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);  // Print the output line by line
                }
            }

            int exitCode = process.waitFor();
            assert exitCode == 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            mvnState = MvnState.EXECUTED;
        }
        log.info("Container image built and pushed for {}", exposedApp.getMetadata().getName());
    }

    public static void copyDirectory(Path sourceDirectory, Path destinationDirectory) throws IOException {
        // Check if destinationDirectory exists, create if not
        if (!Files.exists(destinationDirectory)) {
            Files.createDirectories(destinationDirectory);
        }

        // Stream the directory and process each path found within it
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            paths.forEach(sourcePath -> {
                try {
                    Path targetPath = destinationDirectory.resolve(sourceDirectory.relativize(sourcePath));
                    log.info("Copying {} to {}", sourcePath, targetPath);
                    // Use COPY_ATTRIBUTES to copy the file attributes, including the modification time.
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectory(targetPath);
                        }
                    } else {
                        // REPLACE_EXISTING to overwrite an existing file in the destination.
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public Result<Deployment> match(Deployment actual, ExposedApp primary, Context<ExposedApp> context) {
        final var desiredSpec = primary.getSpec();
        final var container = actual.getSpec().getTemplate().getSpec().getContainers()
                .stream()
                .findFirst();
        return Result.nonComputed(container.map(c -> c.getImage().equals(desiredSpec.getImageRef())
                && desiredSpec.getEnv().equals(convert(c.getEnv()))).orElse(false));
    }

    private Map<String, String> convert(List<EnvVar> envVars) {
        final var result = new HashMap<String, String>(envVars.size());
        envVars.forEach(e -> result.put(e.getName(), e.getValue()));
        return result;
    }
}
