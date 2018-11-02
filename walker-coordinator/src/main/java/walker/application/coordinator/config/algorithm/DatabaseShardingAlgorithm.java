package walker.application.coordinator.config.algorithm;

import java.util.Collection;

import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;
import walker.application.coordinator.CoordinatorConst;

/**
 * Copyright: Copyright (C) github.com/devpage, Inc. All rights reserved. <p>
 *
 * @author SONG
 * @since 2018/10/31 23:18
 */
public class DatabaseShardingAlgorithm implements PreciseShardingAlgorithm<String> {

    @Override
    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<String> shardingValue) {
        for (String each : availableTargetNames) {
            if (each.endsWith(System.identityHashCode(shardingValue) % CoordinatorConst.DATA_SOURCE_SHARDING_COUNT+"")) {
                return each;
            }
        }
        throw new UnsupportedOperationException();
    }
}

