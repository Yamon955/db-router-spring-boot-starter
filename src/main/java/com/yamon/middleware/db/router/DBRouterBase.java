package com.yamon.middleware.db.router;

/**
 * @Description 数据源基础配置
 * @Author Yamon
 * @Create 2023/12/26 12:37
 */
public class DBRouterBase {

    private String tbIdx;

    public String getTbIdx() {
        return DBContextHolder.getTBKey();
    }

}
