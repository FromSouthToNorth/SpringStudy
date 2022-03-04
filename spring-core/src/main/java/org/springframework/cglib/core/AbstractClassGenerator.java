package org.springframework.cglib.core;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Abstract class for all code-generating CGLIB utilities.
 * In addition to caching generated classes for performance, it provides hooks for
 * customizing the <code>ClassLoader</code>, name of the generated class, and transformations
 * applied before generation.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
abstract public class AbstractClassGenerator<T> implements ClassGenerator {

    private static final ThreadLocal CURRENT = new ThreadLocal();

    private static volatile Map<ClassLoader, ClassLoaderData> CACHE = new WeakHashMap<> ();

    private static final boolean DEFAULT_USE_CACHE =
            Boolean.parseBoolean(System.getProperty("cglib.useCache", "true"));

    private GeneratorStrategy strategy = DefaultGeneratorStrategy.INSTANCE;

    private NamingPolicy namingPolicy = DefaultNamingPolicy.INSTANCE;


    protected static class ClassLoaderData {

    }

}
