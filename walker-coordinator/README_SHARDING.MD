分片键算法类型
```
根据数据源策略与表策略、单分片与多分片，
这两种组合，一共产生了4种可供实现的分片算法的接口
```

1. 单分片键数据源分片算法 
SingleKeyDatabaseShardingAlgorithm
1. 单分片表分片算法       
SingleKeyTableShardingAlgorithm
1. 多分片键数据源分片算法 
MultipleKeyDatabaseShardingAlgorithm
1. 多分片表分片算法       
MultipleKeyTableShardingAlgorithm


参考：https://blog.csdn.net/wuliusir/article/details/51090189