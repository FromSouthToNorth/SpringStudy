package org.springframework.util;

import org.springframework.lang.Nullable;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Miscellaneous {@code java.lang.Class} utility methods.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 1.1
 * @see TypeUtils
 * @see ReflectionUtils
 */
public abstract class ClassUtils {

    /** Suffix for array class names: {@code "[]"}. */
    public static final String ARRAY_SUFFIX = "[]";

    /** Prefix for internal array class names: {@code "["}. */
    private static final String INTERNAL_ARRAY_PREFIX = "[";

    /** Prefix for internal non-primitive array class names: {@code "[L"}. */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

    /** A reusable empty class array constant. */
    private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

    /** The package separator character: {@code '.'}. */
    private static final char PACKAGE_SEPARATOR = '.';

    /** The path separator character: {@code '/'}. */
    private static final char PATH_SEPARATOR = '/';

    /** The nested class separator character: {@code '$'}. */
    private static final char NESTED_CLASS_SEPARATOR = '$';

    /** The CGLIB class separator: {@code "$$"}. */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";

    /** The ".class" file suffix. */
    public static final String CLASS_FILE_SUFFIX = ".class";


    /**
     * Map with primitive wrapper type as key and corresponding primitive
     * type as value, for example: Integer.class -> int.class.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

    /**
     * Map with primitive type as key and corresponding wrapper
     * type as value, for example: int.class -> Integer.class.
     */
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

    /**
     * Map with primitive type name as key and corresponding primitive
     * type as value, for example: "int" -> "int.class".
     */
    private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

    /**
     * Map with common Java language class name as key and corresponding Class as value.
     * Primarily for efficient deserialization of remote invocations.
     */
    private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

    /**
     * Common Java language interfaces which are supposed to be ignored
     * when searching for 'primary' user-level interfaces.
     */
    private static final Set<Class<?>> javaLanguageInterfaces;

    /**
     * Cache for equivalent methods on an interface implemented by the declaring class.
     */
    private static final Map<Method, Method> interfaceMethodCache = new ConcurrentReferenceHashMap<>(256);

    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);
        primitiveWrapperTypeMap.put(Void.class, void.class);

        // Map entry iteration is less expensive to initialize than forEach with lambdas
        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
            registerCommonClasses(entry.getKey());
        }

        Set<Class<?>> primitiveTypes = new HashSet<>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
                double[].class, float[].class, int[].class, long[].class, short[].class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }

        registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
                Float[].class, Integer[].class, Long[].class, Short[].class);
        registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
                Class.class, Class[].class, Object.class, Object[].class);
        registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
                Error.class, StackTraceElement.class, StackTraceElement[].class);
        registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
                Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

        Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
                Closeable.class, AutoCloseable.class, Closeable.class, Comparable.class};
        registerCommonClasses(javaLanguageInterfaceArray);
        javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
    }

    /**
     * Register the given common classes with the ClassUtils cache.
     */
    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    /**
     * Return the default ClassLoader to use: typically the thread context
     * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
     * class will be used as fallback.
     * <p>Call this method if you intend to use the thread context ClassLoader
     * in a scenario where you clearly prefer a non-null ClassLoader reference:
     * for example, for class path resource loading (but not necessarily for
     * {@code Class.forName}, which accepts a {@code null} ClassLoader
     * reference as well).
     * @return the default ClassLoader (only {@code null} if even the system
     * ClassLoader isn't accessible)
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getSystemClassLoader()
     */
    @Nullable
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }
        catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                }
                catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }


    /**
     * Replacement for {@code Class.forName()} that also returns Class instances
     * for primitives (e.g. "int") and array class names (e.g. "String[]").
     * Furthermore, it is also capable of resolving nested class names in Java source
     * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
     * @param name the name of the Class
     * @param classLoader the class loader to use
     * (may be {@code null}, which indicates the default class loader)
     * @return a class instance for the supplied name
     * @throws ClassNotFoundException if the class was not found
     * @throws LinkageError if the class file could not be loaded
     * @see Class#forName(String, boolean, ClassLoader)
     */
    public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {

        Assert.notNull(name, "Name must not be null");
        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "[Ljava.lang.String;" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return Class.forName(name, false, clToUse);
        }
        catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String nestedClassName = name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                try {
                    return Class.forName(nestedClassName, false, clToUse);
                }
                catch (ClassNotFoundException ex2) {
                    //  Swallow - let original exception get through
                }
            }
            throw ex;
        }
    }

    /**
     * Resolve the given class name as primitive class, if appropriate,
     * according to the JVM's naming rules for primitive classes.
     * <p>Also supports the JVM's internal class names for primitive arrays.
     * Does <i>not</i> support the "[]" suffix notation for primitive arrays;
     * this is only supported by {@link #forName(String, ClassLoader)}.
     * @param name the name of the potentially primitive class
     * @return the primitive class, or {@code null} if the name does not denote
     * a primitive class or primitive array class
     */
    @Nullable
    public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
        Class<?> result = null;
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length() <= 7) {
            // Could be a primitive - likely.
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }

    /**
     * Determine whether the {@link Class} identified by the supplied name is present
     * and can be loaded. Will return {@code false} if either the class or
     * one of its dependencies is not present or cannot be loaded.
     * @param className the name of the class to check
     * @param classLoader the class loader to use
     * (may be {@code null} which indicates the default class loader)
     * @return whether the specified class is present (including all of its
     * superclasses and interfaces)
     * @throws IllegalStateException if the corresponding class is resolvable but
     * there was a readability mismatch in the inheritance hierarchy of the class
     * (typically a missing dependency declaration in a Jigsaw module definition
     * for a superclass or interface implemented by the class to be checked here)
     */
    public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
        try {
            forName(className, classLoader);
            return true;
        }
        catch (IllegalAccessError err) {
            throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class ["
                    + className + "]: " + err.getMessage(), err);
        }
        catch (Throwable ex) {
            // Typically ClassNotFoundException or NoClassDefFoundError...
            return false;
        }
    }

    public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        try {
            if (clazz.getClassLoader() == classLoader) {
                return true;
            }
        }
        catch (SecurityException ex) {
            // 通过下面的可加载检查
        }

        // 如果可以从给定的 ClassLoader 加载相同的 Class，则可见
        return isLoadable(clazz, classLoader);
    }

    /**
     * Check whether the given class is loadable in the given ClassLoader.
     * @param clazz the class to check (typically an interface)
     * @param classLoader the ClassLoader to check against
     * @since 5.0.6
     */
    public static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
        try {
            return (clazz == classLoader.loadClass(clazz.getName()));
            // 否则：找到同名的不同类
        }
        catch (ClassNotFoundException ex) {
            // 根本没有找到对应的类
            return false;
        }
    }

    /**
     * Resolve the given class if it is a primitive class,
     * returning the corresponding primitive wrapper type instead.
     * @param clazz the class to check
     * @return the original class, or a primitive wrapper for the original primitive type
     */
    public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeNameMap.get(clazz) : clazz);
    }

    /**
     * Check if the right-hand side type may be assigned to the left-hand side
     * type, assuming setting by reflection. Considers primitive wrapper
     * classes as assignable to the corresponding primitive types.
     * @param lhsType the target type
     * @param rhsType the value type that should be assigned to the target type
     * @return if the target type is assignable from the value type
     * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
     */
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        Assert.notNull(lhsType, "Left-hand side type must not be null");
        Assert.notNull(rhsType, "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            Class<?> resolvedPrimitive = primitiveTypeNameMap.get(rhsType);
            return (lhsType == resolvedPrimitive);
        }
        else {
            Class<?> resolvedWrapper = primitiveTypeNameMap.get(rhsType);
            return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
        }
    }

    /**
     * Determine if the given type is assignable from the given value,
     * assuming setting by reflection. Considers primitive wrapper classes
     * as assignable to the corresponding primitive types.
     * @param type the target type
     * @param value the value that should be assigned to the type
     * @return if the type is assignable from the value
     */
    public static boolean isAssignable(Class<?> type, @Nullable Object value) {
        Assert.notNull(type, "Type must not be null");
        return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
    }

    /**
     * Determine if the supplied class is an <em>inner class</em>,
     * i.e. a non-static member of an enclosing class.
     * @return {@code true} if the supplied class is an inner class
     * @since 5.0.5
     * @see Class#isMemberClass()
     */
    public static boolean isInnerClass(Class<?> clazz) {
        return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
    }

    /**
     * Build a String that consists of the names of the classes/interfaces
     * in the given array.
     * <p>Basically like {@code AbstractCollection.toString()}, but stripping
     * the "class "/"interface " prefix before every class name.
     * @param classes an array of Class objects
     * @return a String of form "[com.foo.Bar, com.foo.Baz]"
     * @see java.util.AbstractCollection#toString()
     */
    public static String classNameToString(Class<?>... classes) {
        return classNameToString(Arrays.asList(classes));
    }

    /**
     * Build a String that consists of the names of the classes/interfaces
     * in the given collection.
     * <p>Basically like {@code AbstractCollection.toString()}, but stripping
     * the "class "/"interface " prefix before every class name.
     * @param classes a Collection of Class objects (may be {@code null})
     * @return a String of form "[com.foo.Bar, com.foo.Baz]"
     * @see java.util.AbstractCollection#toString()
     */
    public static String classNameToString(@Nullable Collection<Class<?>> classes) {
        if (CollectionUtils.isEmpty(classes)) {
            return "[]";
        }
        StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
        for (Class<?> clazz : classes) {
            stringJoiner.add(clazz.getName());
        }
        return stringJoiner.toString();
    }

    /**
     * Copy the given {@code Collection} into a {@code Class} array.classNamesToString
     * <p>The {@code Collection} must contain {@code Class} elements only.
     * @param collection the {@code Collection} to copy
     * @return the {@code Class} array
     * @since 3.1
     * @see StringUtils#toStringArray
     */
    public static Class<?>[] toClassArray(@Nullable Collection<Class<?>> collection) {
        return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
    }

    /**
     * Return all interfaces that the given instance implements as an array,
     * including ones implemented by superclasses.
     * @param instance the instance to analyze for interfaces
     * @return all interfaces that the given instance implements as an array
     */
    public static Class<?>[] getAllInterfaces(Object instance) {
        Assert.notNull(instance, "Instance must not be null");
        return getAllInterfacesForClass(instance.getClass());
    }

    /**
     * Return all interfaces that the given class implements as an array,
     * including ones implemented by superclasses.
     * <p>If the class itself is an interface, it gets returned as sole interface.
     * @param clazz the class to analyze for interfaces
     * @return all interfaces that the given object implements as an array
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
        return getAllInterfacesForClass(clazz, null);
    }

    /**
     * Return all interfaces that the given class implements as an array,
     * including ones implemented by superclasses.
     * <p>If the class itself is an interface, it gets returned as sole interface.
     * @param clazz the class to analyze for interfaces
     * @param classLoader the ClassLoader that the interfaces need to be visible in
     * (may be {@code null} when accepting all declared interfaces)
     * @return all interfaces that the given object implements as an array
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
    }

    /**
     * Return all interfaces that the given instance implements as a Set,
     * including ones implemented by superclasses.
     * @param instance the instance to analyze for interfaces
     * @return all interfaces that the given instance implements as a Set
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Object instance) {
        Assert.notNull(instance, "Instance must not be null");
        return getAllInterfacesForClassAsSet(instance.getClass());
    }

    /**
     * Return all interfaces that the given class implements as a Set,
     * including ones implemented by superclasses.
     * <p>If the class itself is an interface, it gets returned as sole interface.
     * @param clazz the class to analyze for interfaces
     * @return all interfaces that the given object implements as a Set
     */
    public static Set<?> getAllInterfacesForClassASSet(Class<?> clazz) {
        return getAllInterfacesForClassAsSet(clazz, null);
    }

    /**
     * Return all interfaces that the given class implements as a Set,
     * including ones implemented by superclasses.
     * <p>If the class itself is an interface, it gets returned as sole interface.
     * @param clazz the class to analyze for interfaces
     * @param classLoader the ClassLoader that the interfaces need to be visible in
     * (may be {@code null} when accepting all declared interfaces)
     * @return all interfaces that the given object implements as a Set
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface() && isVisible(clazz, classLoader)) {
            return Collections.singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null) {
            Class<?>[] ifcs = current.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (isVisible(ifc, classLoader)) {
                    interfaces.add(ifc);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

}
