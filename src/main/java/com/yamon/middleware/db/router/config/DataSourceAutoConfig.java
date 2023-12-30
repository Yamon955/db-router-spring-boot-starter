package com.yamon.middleware.db.router.config;

import com.yamon.middleware.db.router.DBRouterConfig;
import com.yamon.middleware.db.router.DBRouterJoinPoint;
import com.yamon.middleware.db.router.dynamic.DynamicDataSource;
import com.yamon.middleware.db.router.dynamic.DynamicMybatisPlugin;
import com.yamon.middleware.db.router.strategy.IDBRouterStrategy;
import com.yamon.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import com.yamon.middleware.db.router.util.PropertyUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description 数据源配置解析。类的加载顺序笔记；https://t.zsxq.com/0fZELdch7 @double
 * @Author Yamon
 * @Create 2023/12/26 19:42
 */
//@Configuration标注在类上，标注当前类是配置类，替代application.xml
//@Configuration注解标识的类中声明了1个或者多个@Bean方法，Spring容器可以使用这些方法来注入Bean
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    // 数据源配置组
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    // 默认数据源配置
    private Map<String, Object> defaultDataSourceConfig;

    // 分库数量
    private int dbCount;

    // 分表数量
    private int tbCount;

    // 路由字段
    private String routerKey;

    // 执行顺序：6
    @Bean(name = "db-router-point")
    //如果不加@ConditionalOnMissingBean，当你注册多个相同的bean时，会出现异常
    @ConditionalOnMissingBean
    public DBRouterJoinPoint point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy){
        return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
    }

    @Bean
    public DBRouterConfig dbRouterConfig(){
        return new DBRouterConfig(dbCount, tbCount, routerKey);
    }

    // 执行顺序：2
    @Bean
    public Interceptor plugin(){
        return new DynamicMybatisPlugin();
    }

    // 执行顺序：3
    @Bean
    public DataSource dataSource(){
        // 创建数据源
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (String dbInfo : dataSourceMap.keySet()){
            Map<String, Object> objMap = dataSourceMap.get(dbInfo);
            targetDataSources.put(dbInfo, new DriverManagerDataSource(objMap.get("url").toString(), objMap.get("username").toString(), objMap.get("password").toString()));
        }

        // 设置数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(defaultDataSourceConfig.get("url").toString(), defaultDataSourceConfig.get("username").toString(), defaultDataSourceConfig.get("password").toString()));

        return dynamicDataSource;
    }

    // 执行顺序：5
    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig){
        return new DBRouterStrategyHashCode(dbRouterConfig);
    }

    // 执行顺序：4
    /**
     * 创建并配置事务模板的方法。
     *
     * @param dataSource 数据源，用于配置事务管理器。
     * @return 配置好的事务模板。
     */
    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource){
        // 创建数据源事务管理器
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        // 创建事务模板
        TransactionTemplate transactionTemplate = new TransactionTemplate();
        // 设置事务管理器
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        // 将事务传播行为设置为 "PROPAGATION_REQUIRED"，表示如果当前存在事务，则加入该事务；否则，创建一个新的事务。
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

    // 执行顺序：1
    // 会首先执行此方法 --> 通过BeanPostProcess接口中的postProcessBeforeInitialization() 方法保证首先执行此方法
    //实现了EnvironmentAware接口重写setEnvironment方法后，在工程启动时可以获得系统环境变量和application.yaml 的配置文件的变量，存放在 Environment 变量中
    @Override
    public void setEnvironment(Environment environment) {
        String prefix = "mini-db-router.jdbc.datasource.";

        dbCount = Integer.valueOf(environment.getProperty(prefix + "dbCount"));
        tbCount = Integer.valueOf(environment.getProperty(prefix + "tbCount"));
        routerKey = environment.getProperty(prefix + "routerKey");

        // 分库分表数据源
        String dataSources = environment.getProperty(prefix + "list");
        assert dataSources != null;
        for (String dbInfo : dataSources.split(",")){
            Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix + dbInfo, Map.class);
            dataSourceMap.put(dbInfo, dataSourceProps);
        }

        // 默认数据源
        String defaultData = environment.getProperty(prefix + "default");
        defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);
    }
}
