package com.aicreation.exception;

import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.enums.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数验证失败", e);
        
        StringBuilder errorMsg = new StringBuilder("参数验证失败: ");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
        }
        
        return BaseResponse.error(ErrorCodeEnum.PARAM_VALIDATION_ERROR.getCode(), errorMsg.toString());
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> handleBindException(BindException e) {
        log.warn("参数绑定失败", e);
        
        StringBuilder errorMsg = new StringBuilder("参数绑定失败: ");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
        }
        
        return BaseResponse.error(ErrorCodeEnum.PARAM_BIND_ERROR.getCode(), errorMsg.toString());
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束验证失败", e);
        
        StringBuilder errorMsg = new StringBuilder("约束验证失败: ");
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errorMsg.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
        }
        
        return BaseResponse.error(ErrorCodeEnum.CONSTRAINT_VIOLATION_ERROR.getCode(), errorMsg.toString());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage(), e);
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return BaseResponse.error(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "系统内部错误: " + e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage(), e);
        return BaseResponse.error(ErrorCodeEnum.PARAM_ERROR.getCode(), "参数错误: " + e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return BaseResponse.error(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "系统异常: " + e.getMessage());
    }

    /**
     * 处理静态资源未找到异常（例如错误的 Swagger 资源路径）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("静态资源未找到: {}", e.getMessage(), e);
        BaseResponse<Void> response = BaseResponse.error("4040", "资源不存在");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理文件下载异常
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleFileNotFoundException(FileNotFoundException e) {
        log.warn("文件不存在: {}", e.getMessage(), e);
        BaseResponse<Void> response = BaseResponse.error("4040", "文件不存在: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
