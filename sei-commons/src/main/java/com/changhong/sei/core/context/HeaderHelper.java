package com.changhong.sei.core.context;

import com.changhong.sei.core.commoms.constant.Constants;
import com.changhong.sei.util.thread.ThreadLocalHolder;
import com.changhong.sei.util.thread.ThreadLocalUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 实现功能：api远程调用头部信息帮助类
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-23 13:56
 */
public final class HeaderHelper {
    /**
     * helper单实例
     */
    private static HeaderHelper INSTANCE;

    private HeaderHelper() {
    }

    public static HeaderHelper getInstance() {
        // 先判断实例是否存在，若不存在再对类对象进行加锁处理
        if (INSTANCE == null) {
            synchronized (HeaderHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HeaderHelper();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 远程调用
     */
    public Map<String, String> getRequestHeaderInfo() {
        Map<String, String> headers = new HashMap<String, String>(1);
        //通过本地线程变量传递需要远程服务接收的参数
        Map<String, Object> transMap = ThreadLocalHolder.getTranVars();
        if (Objects.nonNull(transMap) && !transMap.isEmpty()) {
            for (Map.Entry<String, Object> entry : transMap.entrySet()) {
                headers.put(ThreadLocalUtil.TRAN_PREFIX + entry.getKey(), (String) entry.getValue());
            }
        }

        // TODO 兼容SEI3.0认证token
        String token = ThreadLocalUtil.getTranVar(ContextUtil.HEADER_TOKEN_KEY);
        if (StringUtils.isNotBlank(token)) {
            headers.put("Authorization", token);
        }
        // 传递调用方
        headers.put(Constants.HEADER_CALLER, ContextUtil.getAppCode());

        return headers;
    }
}
