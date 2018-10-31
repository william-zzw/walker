package walker.application.coordinator.config.algorithm;

import java.util.Collection;

import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;

/**
 * Copyright: Copyright (C) github.com/devpage, Inc. All rights reserved. <p>
 *
 * @author SONG
 * @since 2018/10/31 23:18
 */
public class NotifyTableShardingAlgorithm implements PreciseShardingAlgorithm<String> {

    @Override
    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<String> shardingValue) {
        for (String each : availableTargetNames) {
            if (each.endsWith(System.identityHashCode(shardingValue) % 10 + "")) {
                return each;
            }
        }
        throw new UnsupportedOperationException();
    }
}
