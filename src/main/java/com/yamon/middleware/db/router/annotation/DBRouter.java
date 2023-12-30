package com.yamon.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @Description 路由注解
 * @Author Yamon
 * @Create 2023/12/26 12:38
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouter {

    // 分库分表字段
    String key() default "";
}
