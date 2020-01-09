package com.changhong.sei.example.service;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.context.SessionUser;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.example.BaseUnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * <strong>实现功能:</strong>
 * <p></p>
 *
 * @author 王锦光 wangj
 * @version 1.0.1 2020-01-09 13:42
 */
public class HelloServiceImplTest extends BaseUnitTest {
    @Autowired
    private HelloServiceImpl service;

    @Test
    public void sayHello() {
        String name = "王锦光";
        SessionUser sessionUser = ContextUtil.getSessionUser();
        Assert.assertNotNull(sessionUser);
        System.out.println(JsonUtils.toJson(sessionUser));
        ResultData result = service.sayHello(name);
        System.out.println(JsonUtils.toJson(result));
    }
}