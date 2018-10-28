package walker.protocol.transaction;

public enum GlobalTransactionStatus {

    /*申请分布式事务ID*/
    INIT,
    /*所有分支开始提交*/
    COMMITTING,
    /*全部已提交*/
    COMMITED,
    /*所有分支开始回滚*/
    CANNCELING,
    /*全部已回滚*/
    CANNELED,
    /*过期后,就不再继续事务了*/
    EXPIRED
}
