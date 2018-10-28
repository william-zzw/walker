package walker.protocol.message;

public class RabbitConst {

    public final static String RABBITMQ_ADMIN_NAME = "rabbitAdmin";

    public final static String EXCHANGE_NAME = "walker.tx.exchange";
    public final static String EXCHANGE_TYPE_TOPIC = "topic";

    public final static String WALKER_TRANSACTION_QUEUE = "walker.transaction";
    public final static String WALKER_COMPENSATE_QUEUE = "walker.compensate";


    /* routes */
//    public final static String ROUTE_FOR_MASTER_TX_REG = "route_for_master_tx_reg";
//    public final static String ROUTE_FOR_MASTER_TX_COMMIT = "route_for_master_tx_commit";
//    public final static String ROUTE_FOR_BRANCH_TX_REG = "route_for_branch_tx_reg";
//    public final static String ROUTE_FOR_BRANCH_TX_COMMIT = "route_for_branch_tx_commit";

    public static final String ROUTE_TO_REPORT_TRANSACTION = "ROUTE_TO_REPORT_TRANSACTION";
    public static final String ROUTE_TO_REPORT_COMPENSATE = "ROUTE_TO_REPORT_COMPENSATE";
}
