package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.core.SerializableTypeWrapper.TypeProvider;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 *
 * <p>{@code ResolvableTypes} may be obtained from {@link #forField(Field) fields},
 * {@link #forMethodParameter(Method, int) method parameters},
 * {@link #forMethodReturnType(Method) method returns} or
 * {@link #forClass(Class) classes}. Most methods on this class will themselves return
 * {@link ResolvableType ResolvableTypes}, allowing easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.0
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturnType(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 * @see ResolvableTypeProvider
 */
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

    /**
     * {@code ResolvableType} returned when no value is available. {@code NONE} is used
     * in preference to {@code null} so that multiple method calls can be safely chained.
     */
    public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

    public static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

    private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
            new ConcurrentReferenceHashMap<>(256);

    /**
     * The underlying Java type being managed.
     */
    private final Type type;

    /**
     * Optional provider for the type.
     */
    @Nullable
    private final TypeProvider typeProvider;

    /**
     * The {@code VariableResolver} to use or {@code null} if no resolver is available.
     */
    @Nullable
    private final VariableResolver variableResolver;

    /**
     * The component type for an array or {@code null} if the type should be deduced.
     */
    @Nullable
    private final ResolvableType componentType;

    @Nullable
    private final Integer hash;

    @Nullable
    private Class<?> resolved;

    @Nullable
    private volatile ResolvableType superType;

    @Nullable
    private volatile ResolvableType[] interfaces;

    @Nullable
    private volatile ResolvableType[] generics;

    /**
     * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
     * with no upfront resolution.
     */
    private ResolvableType(
            Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = calculateHashCode();
        this.resolved = null;
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
     * with upfront resolution and a pre-calculated hash.
     * @since 4.2
     */
    private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
            @Nullable VariableResolver variableResolver, @Nullable Integer hash) {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = null;
        this.hash = hash;
        this.resolved = resolveClass();
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
     * with upfront resolution but lazily calculated hash.
     */
    private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
            @Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.hash = null;
        this.resolved = resolveClass();
    }

    /**
     * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
     * Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
     * @since 4.2
     */
    private ResolvableType(@Nullable Class<?> clazz) {
        this.resolved = (clazz != null ? clazz : Object.class);
        this.type = this.resolved;
        this.typeProvider = null;
        this.variableResolver = null;
        this.componentType = null;
        this.hash = null;
    }

    /**
     * Return the underling Java {@link Type} being managed.
     */
    public Type getType() {
        return SerializableTypeWrapper.unwrap(this.type);
    }

    /**
     * Return the underlying Java {@link Class} being managed, if available;
     * otherwise {@code null}.
     */
    @Nullable
    public Class<?> getRawClass() {
        if (this.type == this.resolved) {
            return this.resolved;
        }
        Type rawType = this.type;
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType).getRawType();
        }
        return (rawType instanceof Class ? (Class<?>) rawType : null);
    }

    /**
     * Return the underlying source of the resolvable type. Will return a {@link Field},
     * {@link MethodParameter} or {@link Type} depending on how the {@link ResolvableType}
     * was constructed. With the exception of the {@link #NONE} constant, this method will
     * never return {@code null}. This method is primarily to provide access to additional
     * type information or meta-data that alternative JVM languages may provide.
     */
    public Object getSource() {
        Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
        return (source != null ? source : this.type);
    }

    /**
     * Return this type as a resolved {@code Class}, falling back to
     * {@link java.lang.Object} if no specific class can be resolved.
     * @return the resolved {@link Class} or the {@code Object} fallback
     * @since 5.1
     * @see #getRawClass()
     * @see #resolve(Class)
     */
    public Class<?> toClass() {
        return resolve(Object.class);
    }

    /**
     * Determine whether the given object is an instance of this {@code ResolvableType}.
     * @param obj the object to check
     * @since 4.2
     * @see #isAssignableFrom(Class)
     */
    public boolean isInstance(@Nullable Object obj) {
        return (obj != null && isAssignableFrom(obj.getClass()));
    }

    /**
     * Determine whether this {@code ResolvableType} is assignable from the
     * specified other type.
     * @param other the type to be checked against (as a {@code Class})
     * @since 4.2
     * @see #isAssignableFrom(ResolvableType)
     */
    public boolean isAssignableFrom(Class<?> other) {
        return isAssignableFrom(forType(other), null);
    }

    /**
     * Determine whether this {@code ResolvableType} is assignable from the
     * specified other type.
     * <p>Attempts to follow the same rules as the Java compiler, considering
     * whether both the {@link #resolve() resolved} {@code Class} is
     * {@link Class#isAssignableFrom(Class) assignable from} the given type
     * as well as whether all {@link #getGenerics() generics} are assignable.
     * @param other the type to be checked against (as a {@code ResolvableType})
     * @return {@code true} if the specified other type can be assigned to this
     * {@code ResolvableType}; {@code false} otherwise
     */
    public boolean isAssignableFrom(ResolvableType other) {
        return isAssignableFrom(other, null);
    }

    public boolean isAssignableFrom(ResolvableType other, @Nullable Map<Type, Type> matchedBefore) {
        Assert.notNull(other, "ResolvableType must not be null");

        // 如果我们不能解析类型，我们就不可分配
        if (this == NONE || other == NONE) {
            return false;
        }

        // 通过委托给组件类型来处理数组
        if (isArray()) {
            return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
        }

        if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
            return true;
        }

        // 处理通配符边界
        WildcardBounds ourBounds = WildcardBounds.get(this);
        WildcardBounds typeBounds = WildcardBounds.get(other);

        // 在形式 X 可分配给 <? extends Number>
        if (typeBounds != null) {
            return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
                    ourBounds.isAssignableFrom(typeBounds.getBounds()));
        }

        // 形式为 <? extends Number> 可分配给 X...
        if (ourBounds != null) {
            return ourBounds.isAssignableFrom(other);
        }

        // 即将进行的主要可分配性检查
        boolean exactMatch = (matchedBefore != null); // 我们现在正在检查嵌套的通用变量...
        boolean checkGenerics = true;
        Class<?> ourResolved = null;
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // 尝试默认可变分辨率
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    ourResolved = resolved.resolve();
                }
            }
            if (ourResolved == null) {
                // 尝试针对目标类型进行可变分辨率
                if (other.variableResolver != null) {
                    ResolvableType resolved = other.variableResolver.resolveVariable(variable);
                    if (resolved != null) {
                        ourResolved = resolved.resolve();
                        checkGenerics = false;
                    }
                }
            }
            if (ourResolved == null) {
                // 未解析的类型变量，可能是嵌套的 -> 从不坚持精确匹配
                exactMatch = false;
            }
        }
        if (ourResolved == null) {
            ourResolved = resolve(Object.class);
        }
        Class<?> otherResolved = other.toClass();

        // 我们需要一个精确的泛型类型匹配
        // List<CharSequence> 不能从 List<String> 分配
        if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
            return false;
        }

        if (checkGenerics) {
            // 递归检查每个泛型
            ResolvableType[] ourGenerics = getGenerics();
            ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
            if (ourGenerics.length != typeGenerics.length) {
                return false;
            }
            if (matchedBefore == null) {
                matchedBefore = new IdentityHashMap<>(1);
            }
            matchedBefore.put(this.type, other.type);
            for (int i = 0; i < ourGenerics.length; i++) {
                if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Return this type as a {@link ResolvableType} of the specified class. Searches
     * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
     * hierarchies to find a match, returning {@link #NONE} if this type does not
     * implement or extend the specified class.
     * @param type the required type (typically narrowed)
     * @return a {@link ResolvableType} representing this object as the specified
     * type, or {@link #NONE} if not resolvable as that type
     * @see #asCollection()
     * @see #asMap()
     * @see #getSuperType()
     * @see #getInterfaces()
     */
    public ResolvableType as(Class<?> type) {
        if (this == NONE) {
            return NONE;
        }
        Class<?> resolved = resolve();
        if (resolved == null || resolved == type) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces()) {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (interfaceAsType != NONE) {
                return interfaceAsType;
            }
        }
        return getSuperType();
    }

    public ResolvableType getSuperType() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return NONE;
        }
        try {
            Type superclass = resolved.getGenericSuperclass();
            if (superclass == null) {
                return NONE;
            }
            ResolvableType superType = this.superType;
            if (superType == null) {
                superType = forType(superclass, this);
                this.superType = superType;
            }
            return superType;
        }
        catch (TypeNotPresentException ex) {
            // 忽略泛型签名中不存在的类型
            return NONE;
        }
    }

    /**
     * Return a {@link ResolvableType} array representing the direct interfaces
     * implemented by this type. If this type does not implement any interfaces an
     * empty array is returned.
     * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
     * @see #getSuperType()
     */
    public ResolvableType[] getInterfaces() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] interfaces = this.interfaces;
        if (interfaces == null) {
            Type[] genericIfcs = resolved.getGenericInterfaces();
            interfaces = new ResolvableType[genericIfcs.length];
            for (int i = 0; i < genericIfcs.length; i++) {
                interfaces[i] = forType(genericIfcs[i], this);
            }
            this.interfaces = interfaces;
        }
        return interfaces;
    }

    /**
     * Return {@code true} if this type resolves to a Class that represents an array.
     * @see #getComponentType()
     */
    public boolean isArray() {
        if (this == NONE) {
            return false;
        }
        return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
                this.type instanceof GenericArrayType || resolveType().isArray());
    }

    /**
     * Return the ResolvableType representing the component type of the array or
     * {@link #NONE} if this type does not represent an array.
     * @see #isArray()
     */
    public ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return this.componentType;
        }
        if (this.type instanceof Class) {
            Class<?> componentType = ((Class<?>) this.type).getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType) {
            return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }


    /**
     * Return a {@link ResolvableType} representing the generic parameter for the
     * given indexes. Indexes are zero based; for example given the type
     * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
     * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
     * for example {@code getGeneric(1, 0)} will access the {@code String} from the
     * nested {@code List}. For convenience, if no indexes are specified the first
     * generic is returned.
     * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
     * @param indexes the indexes that refer to the generic parameter
     * (may be omitted to return the first generic)
     * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
     * @see #hasGenerics()
     * @see #getGenerics()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType getGeneric(@Nullable int... indexes) {
        ResolvableType[] generics = getGenerics();
        if (indexes == null || indexes.length == 0) {
            return (generics.length == 0 ? NONE : generics[0]);
        }
        ResolvableType generic = this;
        for (int index : indexes) {
            generics = generic.getGenerics();
            if (index < 0 || index >= generics.length) {
                return NONE;
            }
            generic = generics[index];
        }
        return generic;
    }

    /**
     * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
     * this type. If no generics are available an empty array is returned. If you need to
     * access a specific generic consider using the {@link #getGeneric(int...)} method as
     * it allows access to nested generics and protects against
     * {@code IndexOutOfBoundsExceptions}.
     * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
     * (never {@code null})
     * @see #hasGenerics()
     * @see #getGeneric(int...)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public ResolvableType[] getGenerics() {
        if (this == NONE) {
            return EMPTY_TYPES_ARRAY;
        }
        ResolvableType[] generics = this.generics;
        if (generics == null) {
            if (this.type instanceof Class) {
                Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
                generics = new ResolvableType[typeParams.length];
                for (int i = 0; i < generics.length; i++) {
                    generics[i] = ResolvableType.forType(typeParams[i], this);
                }
            }
            else if (this.type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
                generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = forType(actualTypeArguments[i], this.variableResolver);
                }
            }
            else {
                generics = resolveType().getGenerics();
            }
            this.generics = generics;
        }
        return generics;
    }

    /**
     * Resolve this type to a {@link java.lang.Class}, returning {@code null}
     * if the type cannot be resolved. This method will consider bounds of
     * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
     * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
     * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
     * returns {@code false}, the given type effectively wraps a plain {@code Class},
     * allowing for plain {@code Class} processing if desirable.
     * @return the resolved {@link Class}, or {@code null} if not resolvable
     * @see #resolve(Class)
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    @Nullable
    public Class<?> resolve() {
        return this.resolved;
    }

    /**
     * Resolve this type to a {@link java.lang.Class}, returning the specified
     * {@code fallback} if the type cannot be resolved. This method will consider bounds
     * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
     * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
     * @param fallback the fallback class to use if resolution fails
     * @return the resolved {@link Class} or the {@code fallback}
     * @see #resolve()
     * @see #resolveGeneric(int...)
     * @see #resolveGenerics()
     */
    public Class<?> resolve(Class<?> fallback) {
        return (this.resolved != null ? this.resolved : fallback);
    }

    /**
     * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
     * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
     * as it cannot be serialized.
     */
    ResolvableType resolveType() {
        if (this.type instanceof ParameterizedType) {
            return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType) {
            Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
            if (resolved == null) {
                resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
            }
            return forType(resolved, this.variableResolver);
        }
        if (this.type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) this.type;
            // 尝试默认变量分辨率
            if (this.variableResolver != null) {
                ResolvableType resolved = this.variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    return resolved;
                }
            }
            // 回退到界限
            return forType(resolveBounds(variable.getBounds()), this.variableResolver);
        }
        return NONE;
    }

    @Nullable
    private Type resolveBounds(Type[] bounds) {
        if (bounds.length == 0 || bounds[0] == Object.class) {
            return null;
        }
        return bounds[0];
    }

    @Nullable
    private Class<?> resolveClass() {
        if (this.type == EmptyType.INSTANCE) {
            return null;
        }
        if (this.type instanceof Class) {
            return (Class<?>) this.type;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolvedComponent = getComponentType().resolve();
            return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
        }
        return resolveType().resolve();
    }

    private int calculateHashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(this.type);
        if (this.typeProvider != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
        }
        if (this.variableResolver != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
        }
        if (this.componentType != null) {
            hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
        }
        return hashCode;
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type}.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     * @param type the source type (potentially {@code null})
     * @return a {@link ResolvableType} for the specified {@link Type}
     * @see #forType(Type, ResolvableType)
     */
    public static ResolvableType forType(@Nullable Type type) {
        return forType(type, null, null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
     * owner type.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     * @param type the source type or {@code null}
     * @param owner the owner type used to resolve variables
     * @return a {@link ResolvableType} for the specified {@link Type} and owner
     * @see #forType(Type)
     */
    public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
        VariableResolver variableResolver = null;
        if (owner != null) {
            variableResolver = owner.variableResolver;
        }
        return forType(type, variableResolver);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}.
     * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
     * @param typeReference the reference to obtain the source type from
     * @return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}
     * @since 4.3.12
     * @see #forType(Type)
     */
    public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
        return forType(typeReference.getType(), null, null);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
     * {@link VariableResolver}.
     * @param type the source type or {@code null}
     * @param variableResolver the variable resolver or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
     */
    static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
        return forType(type, null, variableResolver);
    }

    /**
     * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
     * {@link VariableResolver}.
     * @param type the source type or {@code null}
     * @param typeProvider the type provider or {@code null}
     * @param variableResolver the variable resolver or {@code null}
     * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
     */
    static ResolvableType forType(
            @Nullable Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

        if (type == null && typeProvider != null) {
            type = SerializableTypeWrapper.forTypeProvider(typeProvider);
        }
        if (type == null) {
            return NONE;
        }

        // 对于简单的类引用，立即构建包装器 -
        // 不需要昂贵的分辨率，所以不值得缓存......
        if (type instanceof Class) {
            return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
        }

        // 由于我们没有清理线程等，因此在访问时清除空条目。
        cache.purgeUnreferencedEntries();

        // 检查缓存 - 我们可能有一个之前已经解析过的 ResolvableType...
        ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
        ResolvableType cachedType = cache.get(resultType);
        if (cachedType == null) {
            cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
            cache.put(cachedType, cachedType);
        }
        resultType.resolved = cachedType.resolved;
        return resultType;
    }

    /**
     * Clear the internal {@code ResolvableType}/{@code SerializableTypeWrapper} cache.
     * @since 4.2
     */
    public static void clearCache() {
        cache.clear();
        SerializableTypeWrapper.cache.clear();
    }

    /**
     * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
     */
    interface VariableResolver extends Serializable {

        /**
         * Return the source of the resolver (used for hashCode and equals).
         */
        Object getSource();

        /**
         * Resolve the specified variable.
         * @param variable the variable to resolve
         * @return the resolved variable, or {@code null} if not found
         */
        @Nullable
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }

    /**
     * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
     */
    private static class WildcardBounds {

        private final Kind kind;

        private final ResolvableType[] bounds;

        /**
         * Internal constructor to create a new {@link WildcardBounds} instance.
         * @param kind the kind of bounds
         * @param bounds the bounds
         * @see #get(ResolvableType)
         */
        public WildcardBounds(Kind kind, ResolvableType[] bounds) {
            this.kind = kind;
            this.bounds = bounds;
        }

        /**
         * Return {@code true} if this bounds is the same kind as the specified bounds.
         */
        public boolean isSameKind(WildcardBounds bounds) {
            return this.kind == bounds.kind;
        }

        /**
         * Return {@code true} if this bounds is assignable to all the specified types.
         * @param types the types to test against
         * @return {@code true} if this bounds is assignable to all types
         */
        public boolean isAssignableFrom(ResolvableType... types) {
            for (ResolvableType bound : this.bounds) {
                for (ResolvableType type : types) {
                    if (!isAssignable(bound, type)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isAssignable(ResolvableType source, ResolvableType from) {
            return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
        }

        /**
         * Return the underlying bounds.
         */
        public ResolvableType[] getBounds() {
            return this.bounds;
        }

        /**
         * Get a {@link WildcardBounds} instance for the specified type, returning
         * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
         * @param type the source type
         * @return a {@link WildcardBounds} instance or {@code null}
         */
        @Nullable
        public static WildcardBounds get(ResolvableType type) {
            ResolvableType resolveToWildcard = type;
            while (!(resolveToWildcard.getType() instanceof WildcardBounds)) {
                if (resolveToWildcard == NONE) {
                    return null;
                }
                resolveToWildcard = resolveToWildcard.resolveType();
            }
            WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
            Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
            Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
            ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
            for (int i = 0; i < bounds.length; i++) {
                resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
            }
            return new WildcardBounds(boundsType, resolvableBounds);
        }

        /**
         * The various kinds of bounds.
         */
        enum Kind {UPPER, LOWER}

    }

    /**
     * Internal {@link Type} used to represent an empty value.
     */
    @SuppressWarnings("serial")
    static class EmptyType implements Type, Serializable {

        static final Type INSTANCE = new EmptyType();

        Object readResolve() {
            return INSTANCE;
        }
    }

}
