package com.changhong.sei.core.error;

import com.changhong.sei.core.config.properties.global.GlobalProperties;
import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.exception.WebException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 实现功能：统一异常处理
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-01-16 12:41
 */
@ConditionalOnClass(DispatcherServlet.class)
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionTranslator.class);

    @Autowired
    private GlobalProperties global;

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultData<String> handleError(MissingServletRequestParameterException e) {
        String message = ContextUtil.getMessage("core_global_err_001", e.getParameterName());
        return result(message, e);
    }

    /**
     * 方法参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResultData<String> handleError(MethodArgumentTypeMismatchException e) {
        String message = ContextUtil.getMessage("core_global_err_002", e.getName());
        return result(message, e);
    }

    /**
     * 方法参数无效
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultData<String> handleError(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        assert error != null;
        String message = ContextUtil.getMessage("core_global_err_003", error.getField(), error.getDefaultMessage());
        return result(message, e);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResultData<String> handleError(BindException e) {
        FieldError error = e.getFieldError();
        assert error != null;
        String message = ContextUtil.getMessage("core_global_err_004", error.getField(), error.getDefaultMessage());
        return result(message, e);
    }

    /**
     * 参数验证违反约束
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultData<String> handleError(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String path = ((PathImpl) violation.getPropertyPath()).getLeafNode().getName();
        String message = ContextUtil.getMessage("core_global_err_005", path, violation.getMessage());
        return result(message, e);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResultData<String> handleError(NoHandlerFoundException e) {
        // "404 Not Found"
        String message = ExceptionUtils.getRootCauseMessage(e);
        return result(message, e);
    }

    /**
     * 信息转换错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultData<String> handleError(HttpMessageNotReadableException e) {
        // Message Not Readable
        String message = ExceptionUtils.getRootCauseMessage(e);
        return result(message, e);
    }

    /**
     * Request Method Not Supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultData<String> handleError(HttpRequestMethodNotSupportedException e) {
        // Request Method Not Supported
        String message = ContextUtil.getMessage("core_global_err_006", e.getMethod(), StringUtils.join(e.getSupportedMethods()));
        return result(message, e);
    }

    /**
     * 不支持的媒体类型
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResultData<String> handleError(HttpMediaTypeNotSupportedException e) {
        // Media Type Not Supported
        String message = ExceptionUtils.getRootCauseMessage(e);
        return result(message, e);
    }

    /**
     * 不属于以上异常的其他异常
     */
    @ExceptionHandler(Throwable.class)
    public ResultData<String> handleError(Throwable e) {
        // 获取根异常
        Throwable throwable = ExceptionUtils.getRootCause(e);
        if (throwable instanceof java.sql.SQLException) {
            // SQLException sql异常处理
            return result(ContextUtil.getMessage("core_global_err_007", throwable.getMessage()), e);
        } else if (throwable instanceof OptimisticLockingFailureException) {
            // 并发操作引发乐观锁异常
            return result(ContextUtil.getMessage("core_global_err_008", throwable.getMessage()), e);
        } else {
            return result(throwable.getMessage(), e);
        }
    }

    private ResultData<String> result(String message, Throwable e) {
        LOG.error(message, e);
        // 因sei3.0返回格式不统一,无法按标准6.0版本样返回统一的结构,故直接按异常抛出
        if (global.isCompatible()) {
            throw new WebException(message, e);
        }
        return ResultData.fail(message);
    }
}
