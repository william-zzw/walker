CREATE TABLE `walker_transaction` (
  `id` bigint(20) AUTO_INCREMENT COMMENT '唯一标示',
  `gmt_create` int(11) DEFAULT '0' COMMENT '创建时间',
  `gmt_modified` int(11) DEFAULT '0' COMMENT '修改时间',
  `app_id` varchar(64) DEFAULT NULL COMMENT '名称',
  `master_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `branch_gid` varchar(64) DEFAULT NULL COMMENT '名称',
  `is_declare` bit(1) DEFAULT b'0' COMMENT '1 声明者 2 跟随者',
  `status` int(1) DEFAULT '1' COMMENT '状态 1新增 2 待提交 3 已提交 4 待回滚 5 已回滚',
  `is_deleted` int(1) DEFAULT '0' COMMENT '0 未删除 1 逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;