package org.springframework.core;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ParameterNameDiscoverer} implementation which uses Kotlin's reflection facilities
 * for introspecting parameter names.
 *
 * Compared to {@link StandardReflectionParameterNameDiscoverer}, it allows in addition to
 * determine interface parameter names without requiring Java 8 -parameters compiler flag.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class KotlinReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {

    @Nullable
    @Override
    public String[] getParameterNames(Method method) {
        if (!KotlinDetector.isKotlinType(method.getDeclaringClass())) {
            return null;
        }

        try {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
            return (function != null ? getParameterNames(function.getParameters()) : null);
        }
        catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    @Nullable
    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        if (ctor.getDeclaringClass().isEnum() || !KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
            return null;
        }

        try {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(ctor);
            return (function != null? getParameterNames(function.getParameters()) : null);
        }
        catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    @Nullable
    public String[] getParameterNames(List<KParameter> parameters) {
        List<KParameter> filteredParameters = parameters.stream()
                // 必须包含扩展方法的扩展接收器，因为它们在 Java 中作为普通方法参数出现
                .filter(p -> KParameter.Kind.VALUE.equals(p.getKind()) || KParameter.Kind.EXTENSION_RECEIVER.equals(p.getKind()))
                .collect(Collectors.toList());
        String[] parameterNames = new String[filteredParameters.size()];
        for (int i = 0; i < filteredParameters.size(); i++) {
            KParameter parameter = filteredParameters.get(i);
            String name = KParameter.Kind.EXTENSION_RECEIVER.equals(parameter.getKind()) ? "$receiver" : parameter.getName();
            if (name == null) {
                return null;
            }
            parameterNames[i] = name;
        }
        return parameterNames;
    }

}
