package org.drools.cloudevents.operator;

import java.util.Collections;
import java.util.Map;

public class ExposedAppSpec {

    // Add Spec information here
    private String imageRef;
    private Map<String, String> env;

    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getImageRef() {
        return imageRef;
    }

    public void setImageRef(String imageRef) {
        this.imageRef = imageRef;
    }

    public Map<String, String> getEnv() {
        return env == null ? Collections.emptyMap() : env;
    }
}
