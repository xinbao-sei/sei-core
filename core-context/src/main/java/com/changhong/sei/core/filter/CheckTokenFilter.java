package com.changhong.sei.core.filter;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.context.SessionUser;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.util.JsonUtils;
import com.chonghong.sei.util.thread.ThreadLocalUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * 实现功能：检查token
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-07 15:58
 */
public class CheckTokenFilter extends BaseWebFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (StringUtils.endsWithAny(path,
                "/", "/csrf", "/auth/check", "/auth/getAnonymousToken")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中获取token
        String token = request.getHeader(ContextUtil.HEADER_TOKEN_KEY);
        if (StringUtils.isBlank(token)) {
            // 认证失败
            unauthorized("token为空,认证失败!", response);
            return;
        }

        // 检查token
        SessionUser user;
        try {
            user = ContextUtil.getSessionUser(token);
        } catch (Exception e) {
            // 认证失败
            unauthorized("token不合法,认证失败!", response);
            return;
        }

        // token 解析通过,则认证通过;设置用户信息到当前线程全局变量中
        ThreadLocalUtil.setLocalVar(SessionUser.class.getSimpleName(), user);

        filterChain.doFilter(request, response);
    }

    /**
     * 返回类名，避免filter不被执行
     */
    @Override
    protected String getFilterName() {
        return CheckTokenFilter.class.getSimpleName();
    }

    /**
     * 认证失败
     */
    private void unauthorized(String msg, HttpServletResponse response) throws IOException {
        //认证错误处理
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write(JsonUtils.toJson(ResultData.fail(msg)));
        writer.close();
    }
}