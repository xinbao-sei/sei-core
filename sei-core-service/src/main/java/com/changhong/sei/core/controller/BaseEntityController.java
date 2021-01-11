package com.changhong.sei.core.controller;

import com.changhong.sei.core.api.BaseEntityApi;
import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.BaseEntityDto;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.core.service.bo.OperateResult;
import com.changhong.sei.core.service.bo.OperateResultWithData;
import com.changhong.sei.core.utils.ResultDataUtil;
import com.changhong.sei.exception.WebException;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 实现功能: 一般业务实体服务控制抽象基类
 *
 * @author 王锦光 wangjg
 * @version 2020-03-18 16:46
 */
public abstract class BaseEntityController<T extends BaseEntity, D extends BaseEntityDto>
        implements DefaultBaseController<T, D>, BaseEntityApi<D> {
    // 数据实体类型
    private final Class<T> clazzT;
    // DTO实体类型
    private final Class<D> clazzD;
    /**
     * DTO转换为Entity的转换器
     */
    protected static final ModelMapper entityModelMapper;
    /**
     * Entity转换为DTO的转换器
     */
    protected static final ModelMapper dtoModelMapper;
    // 初始化静态属性
    static {
        // 初始化DTO转换为Entity的转换器
        entityModelMapper = new ModelMapper();
        // 设置为严格匹配
        entityModelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // 初始化Entity转换为DTO的转换器
        dtoModelMapper = new ModelMapper();
    }

    // 构造函数
    @SuppressWarnings("unchecked")
    protected BaseEntityController(){
        ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();
        Type[] genericTypes = parameterizedType.getActualTypeArguments();
        this.clazzT = (Class<T>) genericTypes[0];
        this.clazzD = (Class<D>) genericTypes[1];
        // 执行自定义设置;
        customerConvertToEntityMapper();
        // 执行自定义设置
        customConvertToDtoMapper();
    }

    /**
     * 获取使用的业务逻辑实现
     * @return 业务逻辑
     */
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

    /**
     * 自定义设置DTO转换为Entity的转换器
     */
    protected void customerConvertToEntityMapper() {
    }

    /**
     * 将DTO转换成数据实体
     *
     * @param dto 业务实体
     * @return 数据实体
     */
    @Override
    public T convertToEntity(D dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        return entityModelMapper.map(dto, getEntityClass());
    }

    /**
     * 自定义设置Entity转换为DTO的转换器
     */
    protected void customConvertToDtoMapper() {
    }

    /**
     * 将数据实体转换成DTO
     *
     * @param entity 业务实体
     * @return DTO
     */
    @Override
    public D convertToDto(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return dtoModelMapper.map(entity, getDtoClass());
    }

    /**
     * 保存业务实体
     *
     * @param dto 业务实体DTO
     * @return 操作结果
     */
//    @PostMapping(path = "save", consumes = MediaType.APPLICATION_JSON_VALUE)
//    @ApiOperation(value = "保存业务实体", notes = "保存一个业务实体")
    @Override
    public ResultData<D> save(@RequestBody @Valid D dto) {
        ResultData<?> checkResult = checkDto(dto);
        if (checkResult.failed()) {
            return ResultData.fail(checkResult.getMessage());
        }
        // 数据转换 to Entity
        T entity = convertToEntity(dto);
        OperateResultWithData<T> result;
        try {
            result = getService().save(entity);
        } catch (Exception e) {
            // 保存业务实体异常！
            throw new WebException(ContextUtil.getMessage("core_service_00003"), e);
        }
        if (result.notSuccessful()) {
            return ResultData.fail(result.getMessage());
        }
        // 数据转换 to DTO
        D resultData = convertToDto(result.getData());
        return ResultData.success(result.getMessage(), resultData);
    }

    /**
     * 删除业务实体
     *
     * @param id 业务实体Id
     * @return 操作结果
     */
//    @DeleteMapping(path = "delete/{id}")
//    @ApiOperation(value = "删除业务实体", notes = "删除一个业务实体")
    @Override
    public ResultData<?> delete(@PathVariable("id") String id) {
        try {
            OperateResult result = getService().delete(id);
            return ResultDataUtil.convertFromOperateResult(result);
        } catch (Exception e) {
            // 删除业务实体异常！
            throw new WebException(ContextUtil.getMessage("core_service_00004"), e);
        }
    }

    /**
     * 通过Id获取一个业务实体
     *
     * @param id 业务实体Id
     * @return 业务实体
     */
//    @GetMapping(path = "findOne")
//    @ApiOperation(value = "获取一个业务实体", notes = "通过Id获取一个业务实体")
    @Override
    public ResultData<D> findOne(@RequestParam("id") String id) {
        T entity;
        try {
            entity = getService().findOne(id);
        } catch (Exception e) {
            // 获取业务实体异常！
            throw new WebException(ContextUtil.getMessage("core_service_00005"), e);
        }
        // 转换数据 to DTO
        D dto = convertToDto(entity);
        return ResultData.success(dto);
    }
}
