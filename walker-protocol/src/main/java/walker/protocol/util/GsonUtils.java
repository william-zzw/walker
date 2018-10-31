package walker.protocol.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class GsonUtils {

    //线程安全的
    private static final Gson GSON;

    static {
        GSON = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .disableHtmlEscaping()
                .create();
    }

    //获取gson解析器
    public static Gson getGson() {
        return GSON;
    }

    //对象转换为json
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    //JSON转换为对象1--普通类型
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    //JSON转换为对象-针对泛型的类型
    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }

}
