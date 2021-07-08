package org.springframework.core;


import org.junit.Test;

import java.lang.reflect.Method;

public class MainTest {

    @Test
    public void methodTest() {
        Method method = null;
        Class<?> clazz = null;
        System.out.println(method + ", " + clazz);
    }

}