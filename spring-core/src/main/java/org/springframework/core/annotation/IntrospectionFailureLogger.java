package org.springframework.core.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

/**
 * Log facade used to handle annotation introspection failures (in particular
 * {@code TypeNotPresentExceptions}). Allows annotation processing to continue,
 * assuming that when Class attribute values are not resolvable the annotation
 * should effectively disappear.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public enum IntrospectionFailureLogger {

    DEBUG {
        @Override
        public boolean isEnabled() {
            return getLogger().isDebugEnabled();
        }
        @Override
        public void log(String message) {
            getLogger().debug(message);
        }
    },

    INFO {
        @Override
        boolean isEnabled() {
            return getLogger().isDebugEnabled();
        }

        @Override
        void log(String message) {
            getLogger().info(message);
        }
    };

    @Nullable
    private static Log logger;

    void log(String message, @Nullable Object source, Exception ex) {
        String on = (source != null ? " on " + source : "");
        log(message + on + ": " + ex);
    }

    abstract boolean isEnabled();

    abstract void log(String message);

    private static Log getLogger() {
        Log logger = IntrospectionFailureLogger.logger;
        if (logger == null) {
            logger = LogFactory.getLog(MergedAnnotations.class);
            IntrospectionFailureLogger.logger = logger;
        }
        return logger;
    }

}
