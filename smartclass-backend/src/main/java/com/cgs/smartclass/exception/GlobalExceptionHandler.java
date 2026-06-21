package com.cgs.smartclass.exception;

import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;

/**
 * 全局异常处理器
*/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "参数校验失败: " + message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求体解析失败");
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse<?> dataAccessExceptionHandler(DataAccessException e) {
        log.error("DataAccessException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "数据库操作异常");
    }

    @ExceptionHandler(SocketTimeoutException.class)
    public BaseResponse<?> socketTimeoutExceptionHandler(SocketTimeoutException e) {
        log.error("SocketTimeoutException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "外部服务超时");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
