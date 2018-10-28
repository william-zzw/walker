package walker.common.util;


import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.UUID;


public class Utility {

    public static int getTimestamp() {
        return (int) Instant.now().getLong(ChronoField.INSTANT_SECONDS);
    }

    public static String randomOrderId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


}
