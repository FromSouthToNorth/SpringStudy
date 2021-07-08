package org.springframework.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ParameterNameDiscoverer} that uses the LocalVariableTable
 * information in the method attributes to discover parameter names. Returns
 * {@code null} if the class file was compiled without debug information.
 *
 * <p>Uses ObjectWeb's ASM library for analyzing class files. Each discoverer instance
 * caches the ASM discovered information for each introspected Class, in a thread-safe
 * manner. It is recommended to reuse ParameterNameDiscoverer instances as far as possible.
 *
 * @author Adrian Colyer
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 2.0
 */
public class LocalVariableTableParameterNameDiscoverer implements ParameterNameDiscoverer {

    private static final Log logger = LogFactory.getLog(LocalVariableTableParameterNameDiscoverer.class);

    // 没有任何调试信息的类的标记对象
    private static final Map<Executable, String[]> NO_DEBUG_INFO_MAP = Collections.emptyMap();

    // 缓存使用嵌套索引（值是映射）来保持顶级缓存的大小相对较小
    private final Map<Class<?>, Map<Executable, String[]>> parameterNameCache = new ConcurrentHashMap<>(32);

    @Nullable
    @Override
    public String[] getParameterNames(Method method) {
        return new String[0];
    }

    @Nullable
    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        return new String[0];
    }

}
