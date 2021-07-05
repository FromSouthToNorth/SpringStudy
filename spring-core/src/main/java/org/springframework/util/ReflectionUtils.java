package org.springframework.util;

import java.lang.reflect.*;
import java.util.Map;

/**
 * Simple utility class for working with the reflection API and handling
 * reflection exceptions.
 *
 * <p>Only intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Costin Leau
 * @author Sam Brannen
 * @author Chris Beams
 * @since 1.2.2
 */
public abstract class ReflectionUtils {

    /**
     * Pre-built MethodFilter that matches all non-bridge non-synthetic methods
     * which are not declared on {@code java.lang.Object}.
     * @since 3.0.5
     */
    public static final MethodFilter USER_DECLARED_METHODS =
            (method -> !method.isBridge() && !method.isSynthetic());

    /**
     * Pre-built FieldFilter that matches all non-static, non-final fields.
     */
    public static final FiledFilter COPYABLE_FIELDS =
            (field -> !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())));

    /**
     * Naming prefix for CGLIB-renamed methods.
     * @see #isCglibRenamedMethod
     */
    private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Cache for {@link Class#getDeclaredMethods()} plus equivalent default methods
     * from Java 8 based interfaces, allowing for fast iteration.
     */
    private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);

    /**
     * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
     */
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);

    // Exception handling

    /**
     * Handle the given reflection exception.
     * <p>Should only be called if no checked exception is expected to be thrown
     * by a target method, or if an error occurs while accessing a method or field.
     * <p>Throws the underlying RuntimeException or Error in case of an
     * InvocationTargetException with such a root cause. Throws an
     * IllegalStateException with an appropriate message or
     * UndeclaredThrowableException otherwise.
     * @param ex the reflection exception to handle
     */
    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchFieldException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method or field: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    /**
     * Handle the given invocation target exception. Should only be called if no
     * checked exception is expected to be thrown by the target method.
     * <p>Throws the underlying RuntimeException or Error in case of such a root
     * cause. Throws an UndeclaredThrowableException otherwise.
     * @param ex the invocation target exception to handle
     */
    public static void handleInvocationTargetException(InvocationTargetException ex) {

    }

    /**
     * Rethrow the given {@link Throwable exception}, which is presumably the
     * <em>target exception</em> of an {@link InvocationTargetException}.
     * Should only be called if no checked exception is expected to be thrown
     * by the target method.
     * <p>Rethrows the underlying exception cast to a {@link RuntimeException} or
     * {@link Error} if appropriate; otherwise, throws an
     * {@link UndeclaredThrowableException}.
     * @param ex the exception to rethrow
     * @throws RuntimeException the rethrown exception
     */
    public static void rethrowRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    /**
     * Rethrow the given {@link Throwable exception}, which is presumably the
     * <em>target exception</em> of an {@link InvocationTargetException}.
     * Should only be called if no checked exception is expected to be thrown
     * by the target method.
     * <p>Rethrows the underlying exception cast to an {@link Exception} or
     * {@link Error} if appropriate; otherwise, throws an
     * {@link UndeclaredThrowableException}.
     * @param ex the exception to rethrow
     * @throws Exception the rethrown exception (in case of a checked exception)
     */
    public static void rethrowException(Throwable ex) throws Exception {
        if (ex instanceof Exception) {
            throw (Exception) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    // Constructor handling

    /**
     * Obtain an accessible constructor for the given class and parameters.
     * @param clazz the clazz to check
     * @param parameterTypes the parameter types of the desired constructor
     * @return the constructor reference
     * @throws NoSuchMethodException if no such constructor exists
     * @since 5.0
     */
    public static <T> Constructor<T> accessibleConstructor(Class<T> clazz, Class<?>... parameterTypes)
            throws NoSuchMethodException {

        Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
        makeAccessible(ctor);
        return ctor;
    }

    /**
     * Make the given constructor accessible, explicitly setting it accessible
     * if necessary. The {@code setAccessible(true)} method is only called
     * when actually necessary, to avoid unnecessary conflicts with a JVM
     * SecurityManager (if active).
     * @param ctor the constructor to make accessible
     * @see java.lang.reflect.Constructor#setAccessible
     */
    @SuppressWarnings("deprecation")  // on JDK 9
    public static void makeAccessible(Constructor<?> ctor) {
        if (!Modifier.isPrivate(ctor.getModifiers()) ||
                !Modifier.isPrivate(ctor.getDeclaringClass().getModifiers()) && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
    }

    /**
     * Callback optionally used to filter methods to be operated on by a method callback.
     */
    @FunctionalInterface
    public interface MethodFilter {

        /**
         * Determine whether the given method matches.
         * @param method the method to check
         */
        boolean matches(Method method);

        /**
         * Create a composite filter based on this filter <em>and</em> the provided filter.
         * <p>If this filter does not match, the next filter will not be applied.
         * @param next the next {@code MethodFilter}
         * @return a composite {@code MethodFilter}
         * @throws IllegalArgumentException if the MethodFilter argument is {@code null}
         * @since 5.3.2
         */
        default MethodFilter and(MethodFilter next) {
            Assert.notNull(next, "Next MethodFilter must not be null");
            return method -> matches(method) && next.matches(method);
        }
    }

    /**
     * Callback interface invoked on each field in the hierarchy.
     */
    @FunctionalInterface
    public interface FiledCallback {

        /**
         * Perform an operation using the given field.
         * @param field the field to operate on
         */
        void doWith(Field field) throws IllegalAccessException, IllegalAccessException;
    }

    /**
     * Callback optionally used to filter fields to be operated on by a field callback.
     */
    @FunctionalInterface
    public interface FiledFilter {

        /**
         * Determine whether the given field matches.
         * @param field the field to check
         */
        boolean matches(Field field);

        /**
         * Create a composite filter based on this filter <em>and</em> the provided filter.
         * <p>If this filter does not match, the next filter will not be applied.
         * @param next the next {@code FieldFilter}
         * @return a composite {@code FieldFilter}
         * @throws IllegalArgumentException if the FieldFilter argument is {@code null}
         * @since 5.3.2
         */
        default FiledFilter and(FiledFilter next) {
            Assert.notNull(next, "Next FieldFilter must not be null");
            return field -> matches(field) && next.matches(field);
        }
    }

}
