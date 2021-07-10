# Annotation
## Nullable
~~~java
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Nullable {
}
~~~
-   @Target `指示注释类型适用的上下文`
    -   方法声明
    -   形参声明
    -   字段声明（包括枚举常量）
    
-   @Retention `指示带注释类型的注释将保留多长时间`
    -   注释保留策略