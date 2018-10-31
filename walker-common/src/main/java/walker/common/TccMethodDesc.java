package walker.common;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TccMethodDesc implements Serializable {

    public TccMethodDesc() {
    }

    public TccMethodDesc(String commitMethodName, String[] commitMethodArgs, String cancelMethodName,
                         String[] cancelMethodArgs) {
        super();
        this.commitMethodName = commitMethodName;
        this.commitMethodArgs = commitMethodArgs;
        this.cancelMethodName = cancelMethodName;
        this.cancelMethodArgs = cancelMethodArgs;
    }

    private String commitMethodName;
    ;
    private String[] commitMethodArgs;

    private String cancelMethodName;
    private String[] cancelMethodArgs;

    public String getCommitMethodName() {
        return commitMethodName;
    }

    public void setCommitMethodName(String commitMethodName) {
        this.commitMethodName = commitMethodName;
    }

    public String[] getCommitMethodArgs() {
        return commitMethodArgs;
    }

    public void setCommitMethodArgs(String[] commitMethodArgs) {
        this.commitMethodArgs = commitMethodArgs;
    }

    public String getCancelMethodName() {
        return cancelMethodName;
    }

    public void setCancelMethodName(String cancelMethodName) {
        this.cancelMethodName = cancelMethodName;
    }

    public String[] getCancelMethodArgs() {
        return cancelMethodArgs;
    }

    public void setCancelMethodArgs(String[] cancelMethodArgs) {
        this.cancelMethodArgs = cancelMethodArgs;
    }

}
