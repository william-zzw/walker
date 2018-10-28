package walker.protocol.compensate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@SuppressWarnings("serial")
@Setter
@Getter
@ToString
public class Participant implements Serializable{

    private String globalTransactionGid;
    
    private String branchTransactionGid;
    
    private String url;
    
    private Object requestBody;
    
    private String callback;
    
    public Participant() {}
    
    public static Participant create(String globalGid, String branchGid , String url, Object requestBody, String callback) {
        return new Participant(globalGid, branchGid, url, requestBody, callback);
    }
    
    public Participant(String globalTransactionGid, String branchTransactionGid, String url, Object requestBody,
            String callback) {
        super();
        this.globalTransactionGid = globalTransactionGid;
        this.branchTransactionGid = branchTransactionGid;
        this.url = url;
        this.requestBody = requestBody;
        this.callback = callback;
    }

}
