package org.springframework.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.*;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
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

    @Nullable
    private String[] doGetParameterNames(Executable executable) {
        Class<?> declaringClass = executable.getDeclaringClass();
        Map<Executable, String[]> map = this.parameterNameCache.computeIfAbsent(declaringClass, this::inspectClass);
        return (map != NO_DEBUG_INFO_MAP ? map.get(executable) : null);
    }

    private Map<Executable, String[]> inspectClass(Class<?> clazz) {
        InputStream is = clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz));
        if (is == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot find '.class' file for class [" + clazz +
                        "] - unable to determine constructor/method parameter names");
            }
            return NO_DEBUG_INFO_MAP;
        }
        try {
            ClassReader classReader = new ClassReader(is);
            Map<Executable, String[]> map = new ConcurrentHashMap<>(32);
            classReader.accept(new ParameterNameDiscoveringVisitor(clazz, map), 0);
            return map;
        }
        catch (IOException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception thrown while reading '.class' file for class [" + clazz +
                        "] - unable to determine constructor/method parameter names", ex);
            }
        }
        catch (IllegalArgumentException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("ASM ClassReader failed to parse class file [" + clazz +
                        "], probably due to a new Java class file version that isn't supported yet " +
                        "- unable to determine constructor/method parameter names", ex);
            }
        }
        finally {
            try {
                is.close();
            }
            catch (IOException ex) {
                // ignore
            }
        }
        return NO_DEBUG_INFO_MAP;
    }

    /**
     * Helper class that inspects all methods and constructors and then
     * attempts to find the parameter names for the given {@link Executable}.
     */
    private static class ParameterNameDiscoveringVisitor extends ClassVisitor {

        private static final String STATIC_CLASS_INIT = "<clinit>";

        private final Class<?> clazz;

        private final Map<Executable, String[]> executableMap;

        public ParameterNameDiscoveringVisitor(Class<?> clazz, Map<Executable, String[]> executableMap) {
            super(SpringAsmInfo.ASM_VERSION);
            this.clazz = clazz;
            this.executableMap = executableMap;
        }

        @Override
        @Nullable
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // exclude synthetic + bridged && static class initialization
            if (!isSyntheticOrBridged(access) && !STATIC_CLASS_INIT.equals(name)) {
                return new LocalVariableTableVisitor(this.clazz, this.executableMap, name, desc, isStatic(access));
            }
            return null;
        }

        private static boolean isSyntheticOrBridged(int access) {
            return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
        }

        private static boolean isStatic(int access) {
            return ((access & Opcodes.ACC_STATIC) > 0);
        }

    }

    private static class LocalVariableTableVisitor extends MethodVisitor {

        private static final String CONSTRUCTOR = "<init>";

        private final Class<?> clazz;

        private final Map<Executable, String[]> executableMap;

        private final String name;

        private final org.springframework.asm.Type[] args;

        private final String[] parameterNames;

        private final boolean isStatic;

        private boolean hasLvtInfo = false;

        /*
         * The nth entry contains the slot index of the LVT table entry holding the
         * argument name for the nth parameter.
         */
        private final int[] lvtSlotIndex;

        public LocalVariableTableVisitor(Class<?> clazz, Map<Executable, String[]> map, String name, String desc, boolean isStatic) {
            super(SpringAsmInfo.ASM_VERSION);
            this.clazz = clazz;
            this.executableMap = map;
            this.name = name;
            this.args = Type.getArgumentTypes(desc);
            this.parameterNames = new String[this.args.length];
            this.isStatic = isStatic;
            this.lvtSlotIndex = computeLvtSlotIndices(isStatic, this.args);
        }

        @Override
        public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
            this.hasLvtInfo = true;
            for (int i = 0; i < this.lvtSlotIndex.length; i++) {
                if (this.lvtSlotIndex[i] == index) {
                    this.parameterNames[i] = name;
                }
            }
        }

        @Override
        public void visitEnd() {
            if (this.hasLvtInfo || (this.isStatic && this.parameterNames.length == 0)) {
                // 永远不会为静态无参数方法调用visitLocalVariable
                // 它不使用任何局部变量。
                // 这意味着 hasLvtInfo 对于那种方法可能是假的
                // 即使该类具有局部变量信息。
                this.executableMap.put(resolveExecutable(), this.parameterNames);
            }
        }

        private Executable resolveExecutable() {
            ClassLoader loader = this.clazz.getClassLoader();
            Class<?>[] argTypes = new Class<?>[this.args.length];
            for (int i = 0; i < this.args.length; i++) {
                argTypes[i] = ClassUtils.resolveClassName(this.args[i].getClassName(), loader);
            }
            try {
                if(CONSTRUCTOR.equals(this.name)) {
                    return this.clazz.getDeclaredConstructor(argTypes);
                }
                return this.clazz.getDeclaredMethod(this.name, argTypes);
            }
            catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Method [" + this.name +
                        "] was discovered in the .class file but cannot be resolved in the class object", ex);
            }
        }

        private static int[] computeLvtSlotIndices(boolean isStatic, Type[] paramTypes) {
            int[] lvtIndex = new int[paramTypes.length];
            int nextIndex = (isStatic ? 0 : 1);
            for (int i = 0; i < paramTypes.length; i++) {
                lvtIndex[i] = nextIndex;
                if (isWideType(paramTypes[i])) {
                    nextIndex += 2;
                }
                else {
                    nextIndex ++;
                }
            }
            return lvtIndex;
        }

        private static boolean isWideType(Type aType) {
            // float 不是宽类型
            return (aType == Type.LONG_TYPE || aType == Type.BOOLEAN_TYPE);
        }
    }

}
