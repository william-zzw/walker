package walker.protocol;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;

/**
 * 1xx：相关信息
 * 2xx：操作成功
 * 3xx：重定向
 * 4xx：客户端错误
 * 5xx：服务器错误
 */
@Getter
public enum WalkerCode {

    SUCCESS(200, "OK"),
    INVALID_PARAMS_CONVERSION(402, "参数类型非法"),
    INVALID_REQUEST_MODEL(403, "含有无效参数"),
    REQUEST_METHOD_NOT_SUPPORTED(412, "不支持的HTTP请求方法"),
    RESOURCE_NOT_EXISTS(42004, "资源不存在"),
    INSUFFICIENT_PRODUCT(42005, "库存不足"),
    INSUFFICIENT_BALANCE(42006, "余额不足"),
    SYSTEM_ERROR(500, "服务端异常, 请稍后再试"),
    UPDATE_STATUS_FAIL(5, "状态更新失败");

    private final int code;

    private final String message;

    private static final ImmutableMap<Integer, WalkerCode> CACHE;

    static {
        final ImmutableMap.Builder<Integer, WalkerCode> builder = ImmutableMap.builder();
        for (WalkerCode statusCode : values()) {
            builder.put(statusCode.code, statusCode);
        }
        CACHE = builder.build();
    }

    WalkerCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static WalkerCode valueOfCode(int code) {
        final WalkerCode status = CACHE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("No matching constant for [" + code + "]");
        }
        return status;
    }

}
