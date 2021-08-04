package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads} and instantiates
 * factories of a given type from {@value #FACTORIES_RESOURCE_LOCATION} files which
 * may be present in multiple JAR files in the classpath. The {@code spring.factories}
 * file must be in {@link Properties} format, where the key is the fully qualified
 * name of the interface or abstract class, and the value is a comma-separated list of
 * implementation class names. For example:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * where {@code example.MyService} is the name of the interface, and {@code MyServiceImpl1}
 * and {@code MyServiceImpl2} are two implementations.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public final class SpringFactoriesLoader {

    /**
     * The location to look for factories.
     * <p>Can be present in multiple JAR files.
     */
    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

    private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

    static final Map<ClassLoader, Map<String, List<String>>> cache = new ConcurrentReferenceHashMap<>();

    private SpringFactoriesLoader() {
    }

    /**
     * Load and instantiate the factory implementations of the given type from
     * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader.
     * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
     * <p>If a custom instantiation strategy is required, use {@link #loadFactoryNames}
     * to obtain all registered factory names.
     * <p>As of Spring Framework 5.3, if duplicate implementation class names are
     * discovered for a given factory type, only one instance of the duplicated
     * implementation type will be instantiated.
     * @param factoryType the interface or abstract class representing the factory
     * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
     * @throws IllegalArgumentException if any factory implementation class cannot
     * be loaded or if an error occurs while instantiating any factory
     * @see #loadFactoryNames
     */
    public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
        Assert.notNull(factoryType, "'factoryType' must not be null");
        ClassLoader classloaderToUse = classLoader;
        if (classLoader == null) {
            classloaderToUse = SpringFactoriesLoader.class.getClassLoader();
        }
        List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoader);
        if (logger.isTraceEnabled()) {
            logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
        }
        List<T> result = new ArrayList<>(factoryImplementationNames.size());
        for (String factoryImplementationName : factoryImplementationNames) {
            result.add(instantiateFactory(factoryImplementationName, factoryType, classloaderToUse));
        }
        AnnotationAwareOrderComparator.sort(result);
        return result;
    }

    /**
     * Load the fully qualified class names of factory implementations of the
     * given type from {@value #FACTORIES_RESOURCE_LOCATION}, using the given
     * class loader.
     * <p>As of Spring Framework 5.3, if a particular implementation class name
     * is discovered more than once for the given factory type, duplicates will
     * be ignored.
     * @param factoryType the interface or abstract class representing the factory
     * @param classLoader the ClassLoader to use for loading resources; can be
     * {@code null} to use the default
     * @throws IllegalArgumentException if an error occurs while loading factory names
     * @see #loadFactories
     */
    public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
        ClassLoader classLoaderToUse = classLoader;
        if (classLoader == null) {
            classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
        }
        String factoryTypeName = factoryType.getName();
        return loadSpringFactories(classLoaderToUse).getOrDefault(factoryTypeName, Collections.emptyList());
    }

    private static Map<String, List<String>> loadSpringFactories(ClassLoader classLoader) {
        Map<String, List<String>> result = cache.get(classLoader);
        if (result != null) {
            return result;
        }

        result = new HashMap<>();

        try {
            Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
            }
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
        try {
            Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
            if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
                throw new IllegalArgumentException(
                        "Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
            }
            return (T) ReflectionUtils.accessibleConstructor(factoryImplementationClass).newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException(
                "Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
                ex);
        }
    }

}
