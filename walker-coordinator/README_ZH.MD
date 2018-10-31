### 主事务

 ``` 事务注册时  declare 值为true 表示 为分布式事务创建者, 即主事务
   另外如果masterGid和branchGid相同的话 说明该事务上报者就是事务发起者
   分布式事务的提交与回滚，一切听协调者指挥。  
 ```
 
### 分支事务

 ``` 事务注册时  declare 值为false
   同时这种事务上报注册时 masterGid和branchGid不相同, 只是一个分支事务， 
   这个分支事务可能是被包裹在主事务中(即包裹在同一个Spring事务隔离中,随主事务commit 或 rollbank)，
   也可能是 另外一个JVM容器中 运行着的 一个分布式分支事务
 ```

### 提交与回滚

 ```
 分布式事务中
   【提交】命令 协调者 听从主事务指挥，协调者只有收到主事务的commit 才开始向所有分支(包括主分支这个特殊分支) 推送commit, 
          直到各分支回复协调者 commit OK
   【回滚】命令 在任意一个分支事务上报给协调者后，协调者即开始向各个分支事务服务开始推送rollback,  
          直到各分支回复协调者 rollbank OK
 ```
 
 ### 关于事务(Walker事务)

 ```
   事务记录 和 补偿记录 要在一个库中
   同一个masterGid的分支事务 记录在walker_transaction_$shard_by_master_gid
   补偿记录 按branchGid 记录在notify_$shard_by_branch_gid表 
   execute_status
   当收到master事务的commit请求时 
   
   1. 根据masterGid 更新所有branchGid记录的 执行状态 为 待执行commit, 
       update walker_transaction_$shard_by_master_gid
         set tx_status = 'waite_commit',version = version + 1 
       where master_gid = #{masterGid} and version = $version
       
   2. elastic-job-lite-A DataflowJob fetchData 扫描 所有 待执行commit 的分支事务，得到List<WalkerTransaction>
      select 
       * 
      from walker_transaction_$shard_by_master_gid
       where master_gid = #{masterGid} and tx_status = 'waite_commit' and execute_status = 'waite_doing'
       and MOD(id, shardingTotalCount) = shardingItem
    
   3. elastic-job-lite-A DataflowJob processData 更新这些branchGid的 执行状态 为 正在执行
      update 
       walker_transaction_$shard_by_master_gid
      set
       execute_status = 'doing'
      where  
       master_gid = #{masterGid}
       and branch_gid in [ #{branchGid} ]
   
   4. elastic-job-lite-B DataflowJob fetchData 扫描 所有待执行 commitCompensate 的notify记录
      
      select * from notify_$shard_by_branch_gid where mastetr_id = #{mastetGid} 
      and branch_gid = #{branchGid} and notify_type = 'commit' and 
      and MOD(id, shardingTotalCount) = shardingItem
      
     elastic-job-lite-B DataflowJob processData 
     
      notify_$shard_by_branchGid.foreach( s -> {
         @Transaction(乐观锁)
          do commit compensate OK, 直到返回OK 否则一直重试, 
          update notify_$shard_by_branchGid set notify_status = '通知成功' where branch_gid = #{branchGid};
          update walker_transaction_$shard_by_masterGid set tx_status = 'commited',execute_status='done'
          where branch_gid = #{branchGid}
      });
   
 ```
