package org.springframework.beans;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaBeans 的静态便利方法：用于实例化 bean，
 * 检查 bean 属性类型、复制 bean 属性等
 *
 * <p>主要供框架内部使用，但在某种程度上也
 * 对应用程序类很有用。考虑
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>,
 * <a href="https://hotelsdotcom.github.io/bull/">BULL - Bean Utils Light Library</a>,
 * 或类似的第三方框架，以获得更全面的 bean 实用程序
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

    private static final Log logger = LogFactory.getLog(BeanUtils.class);

    private static final ParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    private static final Set<Class<?>> unknownEditorTypes =
            Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

    static {
        Map<Class<?>, Object> values = new HashMap<>();
        values.put(boolean.class, false);
        values.put(byte.class, (byte) 0);
        values.put(short.class, (short) 0);
        values.put(int.class, 0);
        values.put(long.class, (long) 0);
        DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
    }

    /**
     * Convenience method to instantiate a class using its no-arg constructor.
     * @param clazz class to instantiate
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     * @deprecated as of Spring 5.0, following the deprecation of
     * {@link Class#newInstance()} in JDK 9
     * @see Class#newInstance()
     */
    @Deprecated
    public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must ont be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class in an interface");
        }
        try {
            return clazz.newInstance();
        }
        catch (InstantiationException ex) {
            throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
        }
        catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
        }
    }

    /**
     * Instantiate a class using its 'primary' constructor (for Kotlin classes,
     * potentially having default arguments declared) or its default constructor
     * (for regular Java classes, expecting a standard no-arg setup).
     * <p>Note that this method tries to set the constructor accessible
     * if given a non-accessible (that is, non-public) constructor.
     * @param clazz the class to instantiate
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated.
     * The cause may notably indicate a {@link NoSuchMethodException} if no
     * primary/default constructor was found, a {@link NoClassDefFoundError}
     * or other {@link LinkageError} in case of an unresolvable class definition
     * (e.g. due to a missing dependency at runtime), or an exception thrown
     * from the constructor invocation itself.
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class in an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        }
        catch (NoSuchMethodException ex) {
            Constructor<T> ctor = findPrimaryConstructor(clazz);
            if (ctor != null) {
                return instantiateClass(ctor);
            }
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        }
        catch (LinkageError err) {
            throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
        }
    }

    /**
     * Instantiate a class using its no-arg constructor and return the new instance
     * as the specified assignable type.
     * <p>Useful in cases where the type of the class to instantiate (clazz) is not
     * available, but the type desired (assignableTo) is known.
     * <p>Note that this method tries to set the constructor accessible if given a
     * non-accessible (that is, non-public) constructor.
     * @param clazz class to instantiate
     * @param assignableTo type that clazz must be assignableTo
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     * @see Constructor#newInstance
     */
    @SuppressWarnings("unchecked")
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
       Assert. isAssignable(assignableTo, clazz);
       return (T) instantiateClass(clazz);
    }

    /**
     * Convenience method to instantiate a class using the given constructor.
     * <p>Note that this method tries to set the constructor accessible if given a
     * non-accessible (that is, non-public) constructor, and supports Kotlin classes
     * with optional parameters and default values.
     * @param ctor the constructor to instantiate
     * @param args the constructor arguments to apply (use {@code null} for an unspecified
     * parameter, Kotlin optional parameters and Java primitive types are supported)
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     * @see Constructor#newInstance
     */
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
                return KotlinDelegate.instantiateClass(ctor, args);
            }
            else {
                Class<?>[] parameterTypes = ctor.getParameterTypes();
                Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
                Object[] argsWithDefaultValues = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        Class<?> parameterType = parameterTypes[i];
                        argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                    }
                    else {
                        argsWithDefaultValues[i] = args[i];
                    }
                }
                return ctor.newInstance(argsWithDefaultValues);
            }
        }
        catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
        }
        catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
        }
        catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
        }
        catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex);
        }
    }

    /**
     * Inner class to avoid a hard dependency on Kotlin at runtime.
     */
    private static class KotlinDelegate {

        /**
         * Retrieve the Java constructor corresponding to the Kotlin primary constructor, if any.
         * @param clazz the {@link Class} of the Kotlin class
         * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">
         * https://kotlinlang.org/docs/reference/classes.html#constructors</a>
         */
        @Nullable
        public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
            try {
                KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
                if (primaryCtor == null) {
                    return null;
                }
                Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
                if (constructor == null) {
                    throw new IllegalStateException(
                            "Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
                }
                return constructor;
            }
            catch (UnsupportedOperationException ex) {
                return null;
            }
        }

        /**
         * Instantiate a Kotlin class using the provided constructor.
         * @param ctor the constructor of the Kotlin class to instantiate
         * @param args the constructor arguments to apply
         * (use {@code null} for unspecified parameter if needed)
         */
        public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
                throws IllegalAccessException, InvocationTargetException, InstantiationException {

            KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
            if (kotlinConstructor == null) {
                return ctor.newInstance(args);
            }

            if ((!Modifier.isPrivate(ctor.getModifiers()) || !Modifier.isPrivate(ctor.getDeclaringClass().getModifiers()))) {
                KCallablesJvm.setAccessible(kotlinConstructor, true);
            }

            List<KParameter> parameters = kotlinConstructor.getParameters();
            Map<KParameter, Object> argParameters = CollectionUtils.newHashMap(parameters.size());
            Assert.isTrue(args.length <= parameters.size(),
                    "Number of provided arguments should be less of equals than number of constructor parameters");
            for (int i = 0; i < args.length; i++) {
                if (!(parameters.get(i).isOptional() && args[i] == null)) {
                    argParameters.put(parameters.get(i), args[i]);
                }
            }
            return kotlinConstructor.callBy(argParameters);
        }
    }

    /**
     * Return the primary constructor of the provided class. For Kotlin classes, this
     * returns the Java constructor corresponding to the Kotlin primary constructor
     * (as defined in the Kotlin specification). Otherwise, in particular for non-Kotlin
     * classes, this simply returns {@code null}.
     * @param clazz the class to check
     * @since 5.0
     * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
     */
    @Nullable
    public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
            return KotlinDelegate.findPrimaryConstructor(clazz);
        }
        return null;
    }
}
