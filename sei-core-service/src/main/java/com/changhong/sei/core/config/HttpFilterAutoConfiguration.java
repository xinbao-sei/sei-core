package com.changhong.sei.core.config;

import com.changhong.sei.core.config.properties.http.filter.FilterProperties;
import com.changhong.sei.core.error.GlobalExceptionTranslator;
import com.changhong.sei.core.filter.DefaultSessionUserAuthenticationHandler;
import com.changhong.sei.core.filter.SessionUserAuthenticationHandler;
import com.changhong.sei.core.filter.WebFilter;
import com.changhong.sei.core.filter.WebThreadFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextListener;

import java.util.List;

/**
 * 实现功能：
 * web filter定义接口
 * 可以通过此接口定义filter接口，这样做对应的filter可以在WebThreadFilter之后执行，
 * 并且在spring security之前运行。
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-07 11:35
 */
@Configuration
@ConditionalOnProperty(prefix = "sei.http.filter", name = "enable", havingValue = "true", matchIfMissing = true)
@Import(value = {GlobalExceptionTranslator.class})
@EnableConfigurationProperties({FilterProperties.class})
public class HttpFilterAutoConfiguration {

    @Autowired
    private FilterProperties filterConfig;
    /**
     * 自定义过滤器定义
     */
    @Autowired(required = false)
    private List<WebFilter> filterDefs;

    @Bean
    @ConditionalOnMissingBean
    public SessionUserAuthenticationHandler userAuthenticationHandler() {
        return new DefaultSessionUserAuthenticationHandler(filterConfig.getIgnoreAuthUrl());
    }

    /**
     * 系统内置filter，优先级高于spring security的身份认证
     */
    @Bean
    public FilterRegistrationBean<WebThreadFilter> filterRegistrationBean(Environment environment,
                                                                          SessionUserAuthenticationHandler userAuthenticationHandler) {
        FilterRegistrationBean<WebThreadFilter> registration = new FilterRegistrationBean<>();
        // 拦截所有请求
        registration.addUrlPatterns("/*");

        WebThreadFilter filterProxy = new WebThreadFilter(userAuthenticationHandler, filterDefs);
        filterProxy.setEnvironment(environment);
        registration.setFilter(filterProxy);

        // 设置优先级高于spring security
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }

    /**
     * RequestContextListener监听器
     * (ServletRequestAttributes) RequestContextHolder.getRequestAttributes() 防止为null
     */
    @Bean
    @ConditionalOnMissingBean(RequestContextListener.class)
    public RequestContextListener requestContextListenerBean() {
        return new RequestContextListener();
    }

}
