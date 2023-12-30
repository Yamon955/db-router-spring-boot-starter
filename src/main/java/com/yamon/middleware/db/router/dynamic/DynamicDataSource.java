package com.yamon.middleware.db.router.dynamic;

import com.yamon.middleware.db.router.DBContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @Description 动态数据源获取，每当切换数据源，都要从这个里面进行获取
 * @Author Yamon
 * @Create 2023/12/27 10:37
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    // 如何实现的数据源切换--> AbstractRoutingDataSource --> https://blog.csdn.net/weixin_43002640/article/details/98989716
    @Override
    protected Object determineCurrentLookupKey() {

        // 指定要切换的数据源，即要去那个库执行SQL语句
        return "db" + DBContextHolder.getDBKey();
    }

}
