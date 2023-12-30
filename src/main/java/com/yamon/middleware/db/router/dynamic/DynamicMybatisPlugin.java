package com.yamon.middleware.db.router.dynamic;

import com.yamon.middleware.db.router.DBContextHolder;
import com.yamon.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description Mybatis 拦截器，通过对 SQL 语句的拦截处理，修改分表信息
 * @Author Yamon
 * @Create 2023/12/26 21:34
 */
/*
    @Intercepts：这个注解通常在 MyBatis 中使用，表示一个方法应该被拦截（intercepted）。
    @Signature：这个注解用于定义一个将被拦截的方法签名。
        指定要拦截的方法是名为 "prepare" 的方法，其类型为 StatementHandler。
        该方法接受两个参数，类型分别为 Connection.class 和 Integer.class。

        Method = prepare：在数据库执行前被调用 https://blog.csdn.net/qq_38225558/article/details/85018810
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DynamicMybatisPlugin implements Interceptor {

    // regex 正则表达式 提取表名 拦截from、into、update这种后面跟表名的
    // CASE_INSENSITIVE  启用不区分大小写的匹配。
    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);

    // 此处入参invocation便是指拦截到的对象，即被拦截的方法
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取StatementHandlerintercept
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 通过MetaObject优雅访问对象的属性，这里是访问statementHandler的属性
        MetaObject metaObject = MetaObject.forObject(statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());
        // 先拦截到RoutingStatementHandler，里面有个StatementHandler类型的delegate变量，其实现类是BaseStatementHandler，然后就到BaseStatementHandler的成员变量mappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        String id = mappedStatement.getId();  // id为执行的mapper方法的全路径名
        String className = id.substring(0, id.lastIndexOf("."));
        Class<?> clazz = Class.forName(className);
        // 获取自定义注解判断是否需要进行分表操作
        DBRouterStrategy dbRouterStrategy = clazz.getAnnotation(DBRouterStrategy.class);
        if(null == dbRouterStrategy || !dbRouterStrategy.splitTable()){
            return invocation.proceed(); //放行被拦截方法
        }

        // 获取SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();

        // 替换SQL表名 USER 为 USER_03
        Matcher matcher = pattern.matcher(sql);
        String tableName = null;
        if(matcher.find()){
            tableName = matcher.group().trim();
        }
        assert null != tableName;
        String replaceSql = matcher.replaceAll(tableName + "_" + DBContextHolder.getTBKey());

        // 通过反射修改SQL语句
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, replaceSql); // 即 boundSql.sql = replaceSql;
        field.setAccessible(false);

        // invocation.proceed()的作用就是调用被拦截的方法,即放行
        return invocation.proceed();
    }
}
