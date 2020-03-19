package com.changhong.sei.core.controller;

import com.changhong.sei.core.dto.BaseEntityDto;
import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.core.service.BaseEntityService;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 实现功能: 一般业务实体服务控制抽象基类
 *
 * @author 王锦光 wangjg
 * @version 2020-03-18 16:46
 */
public abstract class BaseEntityController<T extends BaseEntity, D extends BaseEntityDto> implements DefaultBaseEntityController<T, D>{
    // 数据实体类型
    private final Class<T> clazzT;
    // DTO实体类型
    private final Class<D> clazzD;

    // 构造函数
    @SuppressWarnings("unchecked")
    protected BaseEntityController(){
        ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();
        Type[] genericTypes = parameterizedType.getActualTypeArguments();
        this.clazzT = (Class<T>) genericTypes[0];
        this.clazzD = (Class<D>) genericTypes[1];
    }

    @Override
    public abstract BaseEntityService<T> getService();

    /**
     * 获取数据实体的类型
     *
     * @return 类型Class
     */
    @Override
    public Class<T> getEntityClass() {
        return clazzT;
    }

    /**
     * 获取传输实体的类型
     *
     * @return 类型Class
     */
    @Override
    public Class<D> getDtoClass() {
        return clazzD;
    }
}
