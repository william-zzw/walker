package walker.protocol.message.command;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import walker.protocol.compensate.Participant;

public class CompensateCommand {

    public static final int COMMIT = 1;
    public static final int BROKEN = 2;

    @Data
    @ToString
    @Accessors(chain = true)
    public static class CompensateCommit {
        private String appId;
        private String masterGid;
        private String branchGid;
        private Participant commitParticipant;
        private Participant cancelParticipant;
    }

    @Data
    @ToString
    @Accessors(chain = true)
    public static class CompensateBroken {
        private String appId;
        private String masterGid;
        private String branchGid;
        private String exception;
    }

}
