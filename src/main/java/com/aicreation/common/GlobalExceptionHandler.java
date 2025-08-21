package com.aicreation.common;

import com.aicreation.entity.dto.base.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @return 响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        BaseResponse<Void> response = BaseResponse.error(e.getErrorCode(), e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理参数校验异常
     * 
     * @param e 参数校验异常
     * @return 响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        BaseResponse<Void> response = BaseResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理绑定异常
     * 
     * @param e 绑定异常
     * @return 响应对象
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("绑定异常: {}", message);
        BaseResponse<Void> response = BaseResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理约束违反异常
     * 
     * @param e 约束违反异常
     * @return 响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反异常: {}", message);
        BaseResponse<Void> response = BaseResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理系统异常
     * 
     * @param e 系统异常
     * @return 响应对象
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        BaseResponse<Void> response = BaseResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 