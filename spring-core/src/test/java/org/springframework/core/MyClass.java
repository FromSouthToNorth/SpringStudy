package org.springframework.core;

import java.io.Serializable;

public class MyClass implements MyInterface, Serializable {

    private String name;

    private final char sex;

    public MyClass() {
        this.sex = '女';
        name = "null";
    }

    public MyClass(String name) {
        this.name = name;
        this.sex = '女';
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public char getSex() {
        return sex;
    }

    private void privateMethod() {

    }

    @Override
    public String toString() {
        return "MyClass{" +
                "name='" + name + '\'' +
                ", sex=" + sex +
                '}';
    }

    @Override
    public int add(int number) {
        return 0;
    }

}
