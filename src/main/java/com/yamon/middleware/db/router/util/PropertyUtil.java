package com.yamon.middleware.db.router.util;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Description 属性工具类
 * @Author Yamon
 * @Create 2023/12/26 19:50
 */
public class PropertyUtil {

    private static int springBootVersion = 1;

    static {
        try {
            Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
        }catch (ClassNotFoundException e){
            springBootVersion = 2;
        }
    }

    /**
     * Spring Boot 1.x is compatible with Spring Boot 2.x by Using Java Reflect.
     * @param environment the environment context
     * @param prefix the prefix part of property key
     * @param targetClass the target class type of result
     * @return  T
     * @param <T> refer to @param targetClass
     */
    @SuppressWarnings("unchecked")
    public static <T> T handle(final Environment environment, final String prefix, final Class<T> targetClass){
        switch (springBootVersion){
            case 1:
                return (T) v1(environment, prefix);
            default:
                return (T) v2(environment, prefix, targetClass);
        }
    }

    private static Object v1(final Environment environment, final String prefix){
        try {
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            // 使用反射创建了RelaxedPropertyResolver的一个实例，传入了environment作为参数
            Object resolverObject = resolverConstructor.newInstance(environment);
            String prefixParam = prefix.endsWith(".") ? prefix : prefix + ".";
            return getSubPropertiesMethod.invoke(resolverObject, prefixParam);
        }catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                      | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static Object v2(final Environment environment, final String prefix, final Class<?> targetClass) {
        try{
            Class<?> bindClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            Method getMethod = bindClass.getDeclaredMethod("get", Environment.class);
            Method bindMethod = bindClass.getDeclaredMethod("bind", String.class, Class.class);
            Object binderObject = getMethod.invoke(null, environment);
            String prefixParam = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
            Object bindResultObject = bindMethod.invoke(binderObject, prefixParam, targetClass);
            Method resultGetMethod = bindResultObject.getClass().getDeclaredMethod("get");
            return resultGetMethod.invoke(bindResultObject);
        }catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                      | IllegalArgumentException | InvocationTargetException ex){
            throw new RuntimeException(ex.getMessage(), ex);
        }

    }
}

