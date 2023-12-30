package com.yamon.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @Description 路由策略，分表标记
 * @Author Yamon
 * @Create 2023/12/26 12:57
 */

// 明确用户是否需要分库分表,所以需要提供分表注解
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouterStrategy {

    // 标记是否需要分表,默认不分表
    boolean splitTable() default false;

}
