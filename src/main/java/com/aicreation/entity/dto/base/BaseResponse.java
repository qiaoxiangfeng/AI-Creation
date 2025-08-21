package com.aicreation.entity.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author ：李国鹏
 * @Date ：Created in 2:58 PM 2020/1/31
 * @Description：
 * @Modified By：
 * @Version: 1.0.0
 */
@Getter
@Setter
@ToString
@Schema(description = "响应对象")
public class BaseResponse<T>  implements Serializable{

	@Schema(description = "响应码")
    private String code=SUCCESS_CODE;
	@Schema(description = "响应消息")
    private String msg;
	@Schema(description = "响应数据")
    private T  data;

	private static final long serialVersionUID = 1L;
	// 对应的编码与内容(通用的)--默认成功
    private static final String SUCCESS_CODE = "00000000";
    private static final String SUCCESS_MSG = "操作成功！";

    // 对应的编码与内容(通用的)--默认失败
    private static final String FAIL_CODE = "67999999";
    private static final String FAIL_MSG = "操作失败！";
    
    /***一系列构造方法**************************************************/

    public BaseResponse() {
        this.code = SUCCESS_CODE;
        this.msg = SUCCESS_MSG;
    }

    public BaseResponse(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BaseResponse(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public BaseResponse(T data) {
        this.code = SUCCESS_CODE;
        this.msg = SUCCESS_MSG;
        this.data = data;
    }
    

    // 默认成功的返回
    public static <T> BaseResponse<T> defaultSuccessResult() {
        return new BaseResponse<>(SUCCESS_CODE, SUCCESS_MSG);
    }


    // 成功的返回,data为返回的数据
    public static <T> BaseResponse<T> successResult(T data) {
        return new BaseResponse<T>(SUCCESS_CODE, SUCCESS_MSG, data);
    }


    // 默认失败的返回
    public static <T> BaseResponse<T> defaultFailResult() {
        return new BaseResponse<>(FAIL_CODE, FAIL_MSG);
    }

    // 兼容现有代码的静态方法命名
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(code, message, null);
    }
    
    
}
