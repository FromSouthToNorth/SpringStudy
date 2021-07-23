package org.springframework.beans;

import org.springframework.core.SpringProperties;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

/**
 * Internal class that caches JavaBeans {@link java.beans.PropertyDescriptor}
 * information for a Java class. Not intended for direct use by application code.
 *
 * <p>Necessary for Spring's own caching of bean descriptors within the application
 * {@link ClassLoader}, rather than relying on the JDK's system-wide {@link BeanInfo}
 * cache (in order to avoid leaks on individual application shutdown in a shared JVM).
 *
 * <p>Information is cached statically, so we don't need to create new
 * objects of this class for every JavaBean we manipulate. Hence, this class
 * implements the factory design pattern, using a private constructor and
 * a static {@link #forClass(Class)} factory method to obtain instances.
 *
 * <p>Note that for caching to work effectively, some preconditions need to be met:
 * Prefer an arrangement where the Spring jars live in the same ClassLoader as the
 * application classes, which allows for clean caching along with the application's
 * lifecycle in any case. For a web application, consider declaring a local
 * {@link org.springframework.web.util.IntrospectorCleanupListener} in {@code web.xml}
 * in case of a multi-ClassLoader layout, which will allow for effective caching as well.
 *
 * <p>In case of a non-clean ClassLoader arrangement without a cleanup listener having
 * been set up, this class will fall back to a weak-reference-based caching model that
 * recreates much-requested entries every time the garbage collector removed them. In
 * such a scenario, consider the {@link #IGNORE_BEANINFO_PROPERTY_NAME} system property.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 05 May 2001
 * @see #acceptClassLoader(ClassLoader)
 * @see #clearClassLoader(ClassLoader)
 * @see #forClass(Class)
 */
public final class CachedIntrospectionResults {

    /**
     * System property that instructs Spring to use the {@link Introspector#IGNORE_ALL_BEANINFO}
     * mode when calling the JavaBeans {@link Introspector}: "spring.beaninfo.ignore", with a
     * value of "true" skipping the search for {@code BeanInfo} classes (typically for scenarios
     * where no such classes are being defined for beans in the application in the first place).
     * <p>The default is "false", considering all {@code BeanInfo} metadata classes, like for
     * standard {@link Introspector#getBeanInfo(Class)} calls. Consider switching this flag to
     * "true" if you experience repeated ClassLoader access for non-existing {@code BeanInfo}
     * classes, in case such access is expensive on startup or on lazy loading.
     * <p>Note that such an effect may also indicate a scenario where caching doesn't work
     * effectively: Prefer an arrangement where the Spring jars live in the same ClassLoader
     * as the application classes, which allows for clean caching along with the application's
     * lifecycle in any case. For a web application, consider declaring a local
     * {@link org.springframework.web.util.IntrospectorCleanupListener} in {@code web.xml}
     * in case of a multi-ClassLoader layout, which will allow for effective caching as well.
     * @see Introspector#getBeanInfo(Class, int)
     */
    public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";

    private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};

    private static final boolean shouldIntrospectorIgnoreBeaninfoClasses =
            SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);



}
