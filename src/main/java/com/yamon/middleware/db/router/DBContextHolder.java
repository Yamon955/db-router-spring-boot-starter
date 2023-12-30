package com.yamon.middleware.db.router;

/**
 * @Description 数据源上下文
 *      其初始化是在DBRouterStrategyHashCode.doRouter内完成的
 *      而DBRouterStrategyHashCode.doRouter该方法是在定义的切面类DBRouterJoinPoint内完成的
 * @Author Yamon
 * @Create 2023/12/26 12:49
 */public class DBContextHolder {

    private static final ThreadLocal<String> dbKey = new ThreadLocal<String>();
    private static final ThreadLocal<String> tbKey = new ThreadLocal<String>();

    public static void setDBKey(String dbKeyIdx){
        dbKey.set(dbKeyIdx);
    }

    public static String getDBKey(){
        return dbKey.get();
    }

    public static void setTBKey(String tbKeyIdx){
        tbKey.set(tbKeyIdx);
    }

    public static String getTBKey(){
        return tbKey.get();
    }

    public static void clearDBKey(){
        dbKey.remove();
    }

    public static void clearTBKey(){
        tbKey.remove();
    }
}

