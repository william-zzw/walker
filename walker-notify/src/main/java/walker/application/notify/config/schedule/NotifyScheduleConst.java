package walker.application.notify.config.schedule;

/**
 * Copyright: Copyright (C) github.com/devpage, Inc. All rights reserved. <p>
 *
 * @author SONG
 * @since 2018/11/2 20:47
 */
public class NotifyScheduleConst {

    /**
     * JOB 执行停顿时间(微秒)
     */
    public static final int INTERNAL_SLEEP_MICROSECONDS = 1;

    public static final int INTERNAL_MASTER_FETCH_WAITE_STATUS_SIZE = 5;

    /**
     * 并发处理notify 表中 推送TPS
     */
    public static final int INTERNAL_NOTIFY_FETCH_SIZE = 10;

    /**
     *  处理 commit 推送的 分片数量
     */
    public static final int OUTBOUND_COMMIT_NOTIFY_SHARDING_COUNT = 10;

    /**
     * 处理 notify 推送的 分片数量
     */
    public static final int OUTBOUND_CANCEL_NOTIFY_SHARDING_COUNT = 10;

}
