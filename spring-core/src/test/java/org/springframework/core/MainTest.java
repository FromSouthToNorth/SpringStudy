package org.springframework.core;

import org.junit.Test;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MainTest {


    @Test
    public void methodTest() throws InstantiationException, NoSuchMethodException {
//        Object obj = null;
//        Object o = classResult(ClassUtils.class);
//        obj = (classResult(ClassUtils.class) instanceof MyClass ? (MyClass) o : o  );
//        System.out.println("obj = " + obj);
        Object obj = classResult(MyClass.class);

        System.out.println("(obj instanceof MyClass) = " + (obj instanceof MyClass));
        Method[] methods = obj.getClass().getMethods();
        System.out.println("------ getMethods ------ ");
        for (Method method : methods) {
            System.out.println("methodName: " + method.getName());
        }
        System.out.println("------ getDeclaredMethods ------");
        Method[] declaredMethods = obj.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            System.out.println("declaredMethodName: " + declaredMethod.getName());
        }

        Constructor<?> constructor = obj.getClass().getConstructor(String.class);
        System.out.println("constructor: " + constructor.getName());

        System.out.println("------ getSuperclass ------");
        System.out.println("obj.getClass().getSuperclass() = " + obj.getClass().getSuperclass());
        System.out.println("MyInterface.class.getSuperclass() = " + MyInterface.class.getSuperclass());
    }

    @Nullable
    private Object classResult(Class<?> clazz) {
        try {
            return clazz.newInstance();
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("IllegalAccessException class result");
        }
        catch (InstantiationException ex) {
            System.out.println("Java 环境或 Java 应用程序未处于所请求操作的适当状态");
            return null;
        }
    }
}