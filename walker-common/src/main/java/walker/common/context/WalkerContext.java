package walker.common.context;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class WalkerContext {

    public WalkerContext(){}

    public WalkerContext(String masterGid) {
        this.masterGid = masterGid;
    }

    private String masterGid;

    private List<String> branchGids = new LinkedList<String>();

    private AtomicInteger depth = new AtomicInteger(0);

    public void incrementDepth() {
        depth.incrementAndGet();
    }

    public void decrementDepth() {
        depth.decrementAndGet();
    }

    private Throwable exception ;

    private long startTime = System.currentTimeMillis();
}
