package com.changhong.sei.core.api;

import com.changhong.sei.core.dto.BaseEntityDto;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.auth.AuthEntityData;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <strong>实现功能:</strong>
 * <p>权限管理的业务实体API接口</p>
 *
 * @param <T> BaseEntity的子类
 * @author 王锦光(wangj)
 * @version 1.0.1 2017-06-01 17:01
 */
public interface DataAuthEntityApi<T extends BaseEntityDto> {
    /**
     * 通过业务实体Id清单获取数据权限实体清单
     *
     * @param ids 业务实体Id清单
     * @return 数据权限实体清单
     */
    @PostMapping(path = "getAuthEntityDataByIds", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "获取数据权限实体清单", notes = "通过业务实体Id清单获取数据权限实体清单")
    ResultData<List<AuthEntityData>> getAuthEntityDataByIds(@RequestBody List<String> ids);

    /**
     * 获取所有数据权限实体清单
     *
     * @return 数据权限实体清单
     */
    @GetMapping(path = "findAllAuthEntityData")
    @ApiOperation(value = "获取所有数据权限实体清单", notes = "获取当前租户所有数据权限实体清单")
    ResultData<List<AuthEntityData>> findAllAuthEntityData();

    /**
     * 获取当前用户有权限的业务实体清单(未冻结)
     *
     * @param featureCode 功能项代码
     * @return 有权限的业务实体清单
     */
    @GetMapping(path = "getUserAuthorizedEntities")
    @ApiOperation(value = "获取当前用户有权限的业务实体清单", notes = "获取当前用户有权限，并且未冻结的业务实体清单")
    ResultData<List<T>> getUserAuthorizedEntities(@RequestParam(value = "featureCode", required = false, defaultValue = "") String featureCode);
}
