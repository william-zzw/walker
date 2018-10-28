package walker.notify.config;

import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;

import java.util.Collection;

/*
单分片健一致性哈希算法分片设置
 */
public class ConsistentHashingPreciseShardingAlgorithmSharding implements PreciseShardingAlgorithm<String> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        //由一致性hash算法获取表节点
        MurmurConsistHash<String> consistentHash = new MurmurConsistHash<>(availableTargetNames);
        for (String each : availableTargetNames) {
            if (each.endsWith(consistentHash.get(shardingValue.getValue().toString()))) {
                //System.out.println("当前访问节点为："+consistentHash.get(shardingValue.getValue().toString())+"分片健值为："+shardingValue.getColumnName()+":"+shardingValue.getValue().toString());
                return each;
            }
        }
        throw new UnsupportedOperationException();
    }
}
