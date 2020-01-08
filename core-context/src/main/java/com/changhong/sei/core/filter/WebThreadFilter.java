package com.changhong.sei.core.filter;

import com.changhong.sei.core.config.cors.CorsConfig;
import com.changhong.sei.core.context.SessionUser;
import com.chonghong.sei.util.thread.ThreadLocalHolder;
import com.chonghong.sei.util.thread.ThreadLocalUtil;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 实现功能：
 * Web线程拦截器，用于统一处理线程变量
 * 该过滤器执行顺序早于spring security的过滤器
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-07 12:04
 */
public class WebThreadFilter extends BaseCompositeFilterProxy {

    /**
     * 应用上下文
     */
    private ApplicationContext applicationContext;

    /**
     * 应用上下文
     */
    private CorsConfig corsConfig;
    /**
     * 带参数构造器
     */
    public WebThreadFilter(List<WebFilter> filterDefs) {
        super(filterDefs);
    }

    @Override
    protected void initFilterBean() throws ServletException {
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        corsConfig = applicationContext.getBean(CorsConfig.class);
        super.initFilterBean();
    }

    @Override
    protected void handleInnerFilters(List<Filter> innerFilters) {
        super.handleInnerFilters(innerFilters);
        // 跨域
        innerFilters.add(0, new CorsSecurityFilter(corsConfig));
        // 防止XSS攻击
        innerFilters.add(1, new XssFilter());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 初始化
        ThreadLocalHolder.begin();

        // todo 设置token
        SessionUser user = new SessionUser();

        ThreadLocalUtil.setLocalVar(SessionUser.class.getSimpleName(), user);

        try {
            compositeFilter.doFilter(request, response, chain);
        } finally {
            // 释放
            ThreadLocalHolder.end();

            MDC.clear();
        }
    }
}