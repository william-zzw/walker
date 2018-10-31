package walker.core.warpper;

import walker.protocol.WalkerCode;
import walker.protocol.WalkerResult;

public class DefaultTccTransactionResultJudge implements TccTransactionResultJudge {

    @Override
    public boolean ok(Object anyThing) {
        if (anyThing instanceof WalkerResult) {
            WalkerResult result = (WalkerResult) anyThing;
            return result.getCode() == WalkerCode.SUCCESS.getCode();
        }
        return false;
    }
}
