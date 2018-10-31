package walker.common.context;

import org.apache.commons.lang3.StringUtils;
import walker.common.util.Utility;

public class WalkerContextlManager {

    private static InheritableThreadLocal<WalkerContext> TCC_CONTEXT_HOLDER = new NamedThreadLocal<WalkerContext>("walker inheritable thread");

    public static WalkerContext initIfContextNull() {
        if (TCC_CONTEXT_HOLDER.get() == null) {
            TCC_CONTEXT_HOLDER.set(new WalkerContext());
        }
        return TCC_CONTEXT_HOLDER.get();
    }

    public static void clear() {
        TCC_CONTEXT_HOLDER.remove();
    }

    public static WalkerContext getTccContext() {
        initIfContextNull();
        return TCC_CONTEXT_HOLDER.get();
    }

    public static void inherit(String globalGid) {
        initIfContextNull();
        TCC_CONTEXT_HOLDER.get().setMasterGid(globalGid);
    }

    public static boolean isFirst() {
        initIfContextNull();
        return StringUtils.isBlank(getTccContext().getMasterGid());
    }

    public static void startMaster() {
        WalkerContext walkerContext = getTccContext();
        String masterGid = Utility.randomOrderId();
        walkerContext.setMasterGid(masterGid);
        walkerContext.getBranchGids().add(masterGid);
    }

    public static void startBranch() {
        WalkerContext walkerContext = getTccContext();
        walkerContext.getBranchGids().add(Utility.randomOrderId());
    }

    public static WalkerContext getContext() {
        return TCC_CONTEXT_HOLDER.get();
    }

    public static boolean isFirstDepth() {
        return getContext().getDepth().intValue() == 1;
    }

    public static void incrementDepth() {
        getContext().incrementDepth();
    }

    public static void decrementDepth() {
        getContext().decrementDepth();
    }
}
