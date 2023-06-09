package com.changhong.sei.core.config.properties.global;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-07 12:06
 */
@ConfigurationProperties("sei.application")
public class GlobalProperties {
    /**
     * 是否启用调试模式
     */
    private boolean debugger = false;
    /**
     * 应用代码
     */
    private String code;
    /**
     * 描述说明
     */
    private String description;
    /**
     * 应用版本
     */
    private String version;
    /**
     * 当前运行环境
     */
    private String env;
    /**
     * 是否是sei6.0兼容模式
     */
    private boolean compatible = false;

    public boolean isDebugger() {
        return debugger;
    }

    public void setDebugger(boolean debugger) {
        this.debugger = debugger;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }
}
