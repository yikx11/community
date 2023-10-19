package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//注解可以写在方法之上
@Retention(RetentionPolicy.RUNTIME)//声明注解有效时长，程序运行时才有效
public @interface LoginRequired {

}
