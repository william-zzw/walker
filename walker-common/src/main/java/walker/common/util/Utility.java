package walker.common.util;


import java.time.Instant;
import java.util.UUID;


/**
 * @author SONG
 */
public class Utility {


    public static long unix_timestamp(){
        return Instant.now().getEpochSecond();
    }

    public static String randomOrderId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


}
