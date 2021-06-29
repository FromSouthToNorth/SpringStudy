package org.apache.commons.logging.impl;

/**
 * Originally a simple Commons Logging provider configured by system properties.
 * Deprecated in {@code spring-jcl}, effectively equivalent to {@link NoOpLog}.
 *
 * <p>Instead of instantiating this directly, call {@code LogFactory#getLog(Class/String)}
 * which will fall back to {@code java.util.logging} if neither Log4j nor SLF4J are present.
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 * @deprecated in {@code spring-jcl} (effectively equivalent to {@link NoOpLog})
 */
@Deprecated
@SuppressWarnings("serial")
public class SimpleLog extends NoOpLog {

    public SimpleLog(String name) {
        super(name);
        System.out.println(SimpleLog.class.getName() + " is deprecated and equivalent to NoOpLog in spring-jcl. " +
                "Use a standard LogFactory.getLog(Class/String) call instead.");
    }

}
