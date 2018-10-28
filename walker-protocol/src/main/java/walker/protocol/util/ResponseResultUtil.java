package walker.protocol.util;

import walker.protocol.WalkerCode;
import walker.protocol.WalkerResult;

public class ResponseResultUtil {

    /**
     * 请求成功<br>
     * default code: 2000<br>
     * default msg: 请求成功<br>
     * default data: null
     *
     * @return Result
     */
    public static <T> WalkerResult<T> getSuccessResult() {
        return getSuccessResult(null);
    }

    public static <T> WalkerResult<T> getSuccessResult(String message, T data) {
        return new WalkerResult<>(WalkerCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 请求成功<br>
     * default code: 0<br>
     * default msg: 请求成功
     *
     * @param data 数据集合
     * @return Result
     */
    public static <T> WalkerResult<T> getSuccessResult(T data) {
        return new WalkerResult<>(WalkerCode.SUCCESS, data);
    }

    public static <T> WalkerResult<T> getErrorResult() {
        return getErrorResult(WalkerCode.SYSTEM_ERROR, null);
    }

    /**
     * 请求失败
     * default responseCode: 请求失败
     *
     * @return Result
     */
    public static <T> WalkerResult<T> getErrorResult(WalkerCode responseCode) {
        return getErrorResult(responseCode, null);
    }

    /**
     * 请求失败
     *
     * @param responseCode 失败消息
     * @return Result
     */
    public static <T> WalkerResult<T> getErrorResult(WalkerCode responseCode, T data) {
        return getErrorResult(responseCode.getCode(), responseCode.getMessage(), data);
    }

    /**
     * 请求失败
     *
     * @param code 失败返回码
     * @param msg  失败消息
     * @param data 数据集合
     * @return Result
     */
    public static <T> WalkerResult<T> getErrorResult(Integer code, String msg, T data) {
        return new WalkerResult<>(code, msg, data);
    }

}
