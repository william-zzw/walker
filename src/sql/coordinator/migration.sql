CREATE TABLE `walker_transaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标示',
  `gmt_create` bigint(11) DEFAULT '0' COMMENT '创建时间',
  `gmt_modified` bigint(11) DEFAULT '0' COMMENT '修改时间',
  `app_id` varchar(64) DEFAULT NULL COMMENT '名称',
  `master_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `branch_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `is_declare` bit(1) DEFAULT b'0' COMMENT '1 声明者 2 跟随者',
  `tx_status` int(11) DEFAULT '0' COMMENT '状态 0 已保存 1 等待提交 2 提交中 3 已提交 4 等待回滚 5 回滚中 6 已回滚',
  `row_version` int(11) DEFAULT '0' COMMENT '记录版本号,默认从0开始',
  `is_deleted` bit(1) DEFAULT b'0' COMMENT '0 未删除 1 逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `walker_notify` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标示',
  `gmt_create` bigint(11) DEFAULT '0' COMMENT '创建时间',
  `gmt_modified` bigint(11) DEFAULT '0' COMMENT '修改时间',
  `app_id` varchar(64) DEFAULT NULL COMMENT '名称',
  `master_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `branch_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `notify_type` int(11) NOT NULL COMMENT '0 commit 1 cancel',
  `notify_url` varchar(255) DEFAULT NULL COMMENT '推送地址',
  `notify_body` varchar(255) DEFAULT NULL COMMENT '推送内容',
  `notify_status` int(11) DEFAULT '0' COMMENT '状态 0 已入库 1 待通知 2 通知中 3 通知成功 4 返回失败 5 通知异常(网络连接失败等)',
  `retry_num` int(11) DEFAULT NULL COMMENT '重试次数',
  `is_deleted` bit(1) DEFAULT b'0' COMMENT '0 未删除 1 已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;