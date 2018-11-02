package walker.application.notify;

/**
 * @author SONG
 */
public class CoordinatorConst {

    public static final int DATA_SOURCE_SHARDING_COUNT = 2;
    public static final int NOTIFY_SHARDING_COUNT = 10;

    public static final int PROCESS_RECENT_DAY = 3;
    public static final int SECONDS_OF_DAY = 86400;

    public static class TransactionTxStatus {
        /**
         * 保存该条事务信息, 仅仅是一个简单的保存, 没有任何业务意义
         */
        public static final int RECORDED = 0;
        public static final int WAITE_COMMIT = 1;
        public static final int COMMITTING = 2;
        public static final int COMMITED = 3;
        public static final int WAITE_CANCEL = 4;
        public static final int CANCELING = 5;
        public static final int CANCELED = 6;
    }

    public enum  NotifyType {
        /**
         * 提交类型的补偿
         */
        COMMIT,
        /**
         * 回滚类型的补偿
         */
        CANCEL
    }

    public static class NotifyStatus {
        /**
         * 入库
         */
        public static final int RECORDED = 0;
        /**
         * 待通知
         */
        public static final int WAITING_EXECUTE = 1;
        /**
         * 通知中
         */
        public static final int NOTIFYING = 2;
        /**
         * 通知成功
         */
        public static final int NOTIFY_SUCCESS = 3;
        /**
         * 通知失败
         */
        public static final int NOTIFY_FAILURE = 4;
        /**
         * 通知异常(网络连接失败等)
         */
        public static final int NOTIFY_EXCEPTION = 5;

    }
}
