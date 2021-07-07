package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.*;

/**
 * Internal utility class that can be used to obtain wrapped {@link Serializable}
 * variants of {@link java.lang.reflect.Type java.lang.reflect.Types}.
 *
 * <p>{@link #forField(Field) Fields} or {@link #forMethodParameter(MethodParameter)
 * MethodParameters} can be used as the root source for a serializable type.
 * Alternatively, a regular {@link Class} can also be used as source.
 *
 * <p>The returned type will either be a {@link Class} or a serializable proxy of
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable} or
 * {@link WildcardType}. With the exception of {@link Class} (which is final) calls
 * to methods that return further {@link Type Types} (for example
 * {@link GenericArrayType#getGenericComponentType()}) will be automatically wrapped.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class SerializableTypeWrapper {

    private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
            GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};

    static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256);

    private SerializableTypeWrapper() {
    }


    /**
     * Return a {@link Serializable} variant of {@link Field#getGenericType()}.
     */
    @Nullable
    public static Type forField(Field field) {
        return forTypeProvider(new FieldTypeProvider(field));
    }

    /**
     * Unwrap the given type, effectively returning the original non-serializable type.
     * @param type the type to unwrap
     * @return the original non-serializable type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Type> T unwrap(T type) {
        Type unwrapped = null;
        if (type instanceof SerializableTypeProxy) {
            unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
        }
        return (unwrapped != null ? (T) unwrapped : type);
    }

    /**
     * Return a {@link Serializable} {@link Type} backed by a {@link TypeProvider} .
     * <p>If type artifacts are generally not serializable in the current runtime
     * environment, this delegate will simply return the original {@code Type} as-is.
     */
    @Nullable
    static Type forTypeProvider(TypeProvider provider) {
        Type provideType = provider.getType();
        if (provideType == null || provideType instanceof Serializable) {
            // 不需要可序列化的类型包装（例如对于 java.lang.Class）
            return provideType;
        }
        if (NativeDetector.inNativeImage() || !Serializable.class.isAssignableFrom(Class.class)) {
            // 如果类型通常不可序列化，让我们跳过任何包装尝试
            // 当前的运行时环境（甚至 java.lang.Class 本身，例如在 GraalVM 本机图像上）
            return provideType;
        }

        // 获取给定提供者的可序列化类型代理...
        Type cached = cache.get(provideType);
        if (cached != null) {
            return cached;
        }
        for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
            if (type.isInstance(provider)) {
                ClassLoader classLoader = provider.getClass().getClassLoader();
                Class<?>[] interfaces = new Class<?>[] {type, SerializableTypeProxy.class, Serializable.class};
                InvocationHandler handler = new TypeProxyInvocationHandler(provider);
                cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
                cache.put(provideType, cached);
                return cached;
            }
        }
        throw new IllegalArgumentException("Unsupported Type class: " + provideType.getClass().getName());
    }

    /**
     * Additional interface implemented by the type proxy.
     */
    interface SerializableTypeProxy {

        /**
         * Return the underlying type provider.
         */
        TypeProvider getTypeProvider();
    }

    /**
     * A {@link Serializable} interface providing access to a {@link Type}.
     */
    static class FieldTypeProvider implements TypeProvider {

        private final String fieldName;

        private final Class<?> declaringClass;

        private transient Field field;

        public FieldTypeProvider(Field field) {
            this.fieldName = field.getName();
            this.declaringClass = field.getDeclaringClass();
            this.field = field;
        }

        @Override
        public Type getType() {
            return this.field.getGenericType();
        }

        @Override
        public Object getSource() {
            return this.field;
        }

        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            inputStream.defaultReadObject();
            try {
                this.field = this.declaringClass.getDeclaredField(this.fieldName);
            }
            catch (Throwable ex) {
                throw new IllegalStateException("Could not find original class structure", ex);
            }
        }
    }


    /**
     * A {@link Serializable} interface providing access to a {@link Type}.
     */
    @SuppressWarnings("serial")
    interface TypeProvider extends Serializable {

        /**
         * Return the (possibly non {@link Serializable}) {@link Type}.
         */
        @Nullable
        Type getType();

        /**
         * Return the source of the type, or {@code null} if not known.
         * <p>The default implementations returns {@code null}.
         */
        @Nullable
        default Object getSource() {
            return null;
        }
    }

    /**
     * {@link Serializable} {@link InvocationHandler} used by the proxied {@link Type}.
     * Provides serialization support and enhances any methods that return {@code Type}
     * or {@code Type[]}.
     */
    @SuppressWarnings("serial")
    private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {

        private final TypeProvider provider;

        public TypeProxyInvocationHandler(TypeProvider provider) {
            this.provider = provider;
        }

        @Override
        @Nullable
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "equals":
                    Object other = args[0];
                    // 打开速度代理
                    if (other instanceof Type) {
                        other = unwrap((Type) other);
                    }
                    return ObjectUtils.nullSafeEquals(this.provider.getType(), other);
                case "hashCode":
                    return ObjectUtils.nullSafeHashCode(this.provider.getType());
                case "getTypeProvider":
                    return this.provider;
            }
            if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
            }
            else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                Type[] result = new Type[((Type[]) method.invoke(this.provider.getType())).length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
                }
                return result;
            }
            try {
                return method.invoke(this.provider.getType(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    /**
     * {@link TypeProvider} for {@link Type Types} obtained from a {@link MethodParameter}.
     */
    @SuppressWarnings("serial")
    static class MethodParameterTypeProvider implements TypeProvider {

        private final String methodName;

        private final Class<?>[] parameterTypes;

        private final Class<?> declaringClass;

        private final int parameterIndex;

        private transient MethodParameter methodParameter;

        public MethodParameterTypeProvider(MethodParameter methodParameter) {
            this.methodName = (methodParameter.getMethod() != null ? methodParameter.getMethod().getName() : null);
            this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
            this.declaringClass = methodParameter.getDeclaringClass();
            this.parameterIndex = methodParameter.getParameterIndex();
            this.methodParameter = methodParameter;
        }

        @Override
        public Type getType() {
            return this.methodParameter.getGenericParameterType();
        }
    }

    /**
     * {@link TypeProvider} for {@link Type Types} obtained by invoking a no-arg method.
     */
    @SuppressWarnings("serial")
    static class MethodInvokeTypeProvider implements TypeProvider {

        private final TypeProvider provider;

        private final String methodName;

        private final Class<?> declaringClass;

        private final int index;

        private transient Method method;

        @Nullable
        private transient volatile Object result;

        public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
            this.provider = provider;
            this.methodName = method.getName();
            this.declaringClass = method.getDeclaringClass();
            this.index = index;
            this.method = method;
        }

        @Nullable
        @Override
        public Type getType() {
            Object result = this.result;
            if (result == null) {
                // 在提供的类型上延迟调用目标方法
                result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
                // 缓存结果以进一步调用 getType()
                this.result = result;
            }
            return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
        }

        @Nullable
        @Override
        public Object getSource() {
            return TypeProvider.super.getSource();
        }

        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            inputStream.defaultReadObject();
        }
    }

}
