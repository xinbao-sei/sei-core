package com.changhong.sei.core.service;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.BaseEntityDto;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.core.manager.BaseEntityManager;
import org.modelmapper.ModelMapper;

import java.util.Objects;

/**
 * <strong>实现功能:</strong>
 * <p>所有业务的API服务默认实现父接口</p>
 *
 * @author 王锦光 wangj
 * @version 1.0.1 2020-01-07 9:32
 */
public interface DefaultBaseService<T extends BaseEntity, D extends BaseEntityDto> {
    // 注入业务逻辑实现
    BaseEntityManager<T> getManager();

    // 获取实体转换类
    ModelMapper getModelMapper();

    /**
     * 获取数据实体的类型
     *
     * @return 类型Class
     */
    Class<T> getEntityClass();

    /**
     * 获取传输实体的类型
     *
     * @return 类型Class
     */
    Class<D> getDtoClass();

    /**
     * 检查输入的DTO参数是否有效
     *
     * @param dto 数据传输对象
     * @return 检查结果
     */
    default ResultData checkDto(D dto) {
        if (Objects.isNull(dto)) {
            // 输入的数据传输对象为空！
            return ResultData.fail(ContextUtil.getMessage("core_service_00002"));
        }
        // 检查通过！
        return ResultData.success(ContextUtil.getMessage("core_service_00001"));
    }

    /**
     * 将数据实体转换成DTO
     *
     * @param entity 业务实体
     * @return DTO
     */
    default D convertToDto(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getModelMapper().map(entity, getDtoClass());
    }

    /**
     * 将DTO转换成数据实体
     *
     * @param dto 业务实体
     * @return 数据实体
     */
    default T convertToEntity(D dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        return getModelMapper().map(dto, getEntityClass());
    }
}
