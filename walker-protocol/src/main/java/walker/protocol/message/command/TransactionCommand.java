package walker.protocol.message.command;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

public class TransactionCommand {

    public final static int PUBLISH = 1;
    public final static int BROKEN = 2;
    public final static int COMMIT = 3;

    @Data
    @ToString
    @Accessors(chain = true)
    public static class TransactionPublish {
        private String appId;
        private String masterGid;
        private String branchGid;
        private boolean declare;
    }

    @Data
    @ToString
    @Accessors(chain = true)
    public static class TransactionBroken {
        private String appId;
        private String masterGid;
        private String branchGid;
        private String exception;
    }

    @Data
    @ToString
    @Accessors(chain = true)
    public static class TransactionCommit {
        private String appId;
        private String masterGid;
        private String branchGid;

    }

}
