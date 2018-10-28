CREATE TABLE `walker_notify` (
  `id` bigint(20) AUTO_INCREMENT COMMENT '唯一标示',
  `gmt_create` int(11) DEFAULT '0' COMMENT '创建时间',
  `gmt_modified` int(11) DEFAULT '0' COMMENT '修改时间',
  `app_id` varchar(64) DEFAULT NULL COMMENT '名称',
  `master_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `branch_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `notify_url` varchar(255) DEFAULT NULL COMMENT '推送地址',
  `notify_body` varchar(255) DEFAULT NULL COMMENT '推送内容',
  `notify_status` int(1) DEFAULT '0' COMMENT '状态 0 待通知 1 已通知',
  `record_status` int(1) DEFAULT '0' COMMENT '状态 0 未知 1 通知成功 2 返回失败 3 通知异常(网络连接失败等)',
  `retry_num` int(11) DEFAULT NULL COMMENT '重试次数',
  `is_deleted` int(1) DEFAULT '0' COMMENT '0 未删除 1 逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;