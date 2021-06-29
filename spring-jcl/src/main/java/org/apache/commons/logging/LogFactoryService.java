package org.apache.commons.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal subclass of the standard Apache Commons Logging's {@code LogFactory} class,
 * overriding the abstract {@code getInstance} lookup methods. This is just applied in
 * case of the standard {@code commons-logging} jar accidentally ending up on the classpath,
 * with the standard {@code LogFactory} class performing its META-INF service discovery.
 * This implementation simply delegates to Spring's common {@link Log} factory methods.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @deprecated since it is only meant to be used in the above-mentioned fallback scenario
 */
@Deprecated
public class LogFactoryService extends LogFactory {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public Log getInstance(Class<?> clazz) {
        return getInstance(clazz.getName());
    }

    @Override
    public Log getInstance(String name) {
        return LogAdapter.createLog(name);
    }

    // Just in case some code happens to call uncommon Commons Logging methods...

    public void setAttribute(String name, Object value) {
        if (value != null) {
            this.attributes.put(name, value);
        }
        else {
            this.attributes.remove(name);
        }
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String[] getAttributeNames() {
        return this.attributes.keySet().toArray(new String[0]);
    }

    public void release() {
    }
}
