package walker.application.coordinator.config.algorithm;

import java.util.Collection;

import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;

import static walker.application.coordinator.CoordinatorConst.NOTIFY_SHARDING_COUNT;

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
            if (each.endsWith(System.identityHashCode(shardingValue) % NOTIFY_SHARDING_COUNT + "")) {
                return each;
            }
        }
        throw new UnsupportedOperationException();
    }
}
