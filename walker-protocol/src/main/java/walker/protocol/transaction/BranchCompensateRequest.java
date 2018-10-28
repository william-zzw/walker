package walker.protocol.transaction;

import lombok.Data;
import lombok.ToString;
import walker.protocol.compensate.Participant;

import java.io.Serializable;

@SuppressWarnings("serial")
@Data
@ToString
public class BranchCompensateRequest implements Serializable {

    private String appId;
    private Integer createTime;
    private String masterTransactionGid;
    private String branchTransactionGid;
    private BranchTransactionStatus branchTransactionStatus;
    private Participant commitParticipant;
    private Participant cancelParticipant;

}