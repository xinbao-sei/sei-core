package com.changhong.sei.core.cache;

import java.util.Set;
import java.util.function.Function;

/**
 * 实现功能：缓存提供者接口
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-01 09:57
 */
public interface CacheProviderService {

    /**
     * 批量获取key
     * @param keys 以*号模糊获取
     */
    Set<String> keys(String keys);

    /**
     * 查询缓存
     *
     * @param key 缓存键 不可为空
     **/
    <T extends Object> T get(String key);

    /**
     * 查询缓存
     *
     * @param key      缓存键 不可为空
     * @param function 如没有缓存，调用该callable函数返回对象 可为空
     **/
    <T extends Object> T get(String key, Function<String, T> function);

    /**
     * 查询缓存
     *
     * @param key      缓存键 不可为空
     * @param function 如没有缓存，调用该callable函数返回对象 可为空
     * @param funcParm function函数的调用参数
     **/
    <T extends Object, M extends Object> T get(String key, Function<M, T> function, M funcParm);

    /**
     * 查询缓存
     *
     * @param key        缓存键 不可为空
     * @param function   如没有缓存，调用该callable函数返回对象 可为空
     * @param expireTime 过期时间（单位：毫秒） 可为空
     **/
    <T extends Object> T get(String key, Function<String, T> function, Long expireTime);

    /**
     * 查询缓存
     *
     * @param key        缓存键 不可为空
     * @param function   如没有缓存，调用该callable函数返回对象 可为空
     * @param funcParm   function函数的调用参数
     * @param expireTime 过期时间（单位：毫秒） 可为空
     **/
    <T extends Object, M extends Object> T get(String key, Function<M, T> function, M funcParm, Long expireTime);

    /**
     * 设置缓存键值
     *
     * @param key 缓存键 不可为空
     * @param obj 缓存值 不可为空
     **/
    <T extends Object> void set(String key, T obj);

    /**
     * 设置缓存键值
     *
     * @param key        缓存键 不可为空
     * @param obj        缓存值 不可为空
     * @param expireTime 过期时间（单位：毫秒） 可为空
     **/
    <T extends Object> void set(String key, T obj, Long expireTime);

    /**
     * 移除缓存
     *
     * @param key 缓存键 不可为空
     **/
    void remove(String key);

    /**
     * 是否存在缓存
     *
     * @param key 缓存键 不可为空
     **/
    boolean contains(String key);
}
