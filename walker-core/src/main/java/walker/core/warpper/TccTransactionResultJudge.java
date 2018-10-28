package walker.core.warpper;

@FunctionalInterface
public interface TccTransactionResultJudge<AnyResult> {

    boolean ok(AnyResult anyThing);

}