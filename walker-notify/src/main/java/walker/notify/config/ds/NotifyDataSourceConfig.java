package walker.notify.config.ds;

import com.zaxxer.hikari.HikariDataSource;
import io.shardingsphere.api.config.ShardingRuleConfiguration;
import io.shardingsphere.api.config.TableRuleConfiguration;
import io.shardingsphere.api.config.strategy.StandardShardingStrategyConfiguration;
import io.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import walker.notify.config.ConsistentHashingPreciseShardingAlgorithmSharding;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
// 扫描 Mapper 接口并容器管理
@MapperScan(basePackages = NotifyDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "notifySqlSessionFactory")
public class NotifyDataSourceConfig {

    // 配置使用该数据源的包和sql
    static final String PACKAGE = "walker.application.notify.mapper";
    static final String MAPPER_LOCATION = "classpath:mapper/walker/application/notify/*.xml";

    @Value("${notify.datasource.url}")
    private String url;

    @Value("${notify.datasource.username}")
    private String user;

    @Value("${notify.datasource.password}")
    private String password;

    @Value("${notify.datasource.driverClassName}")
    private String driverClass;

    @Bean(name = "notifyDataSource")
    public DataSource notifyDataSource() throws SQLException {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTableRuleConfigs().add(getOrderTableRuleConfiguration());
        shardingRuleConfig.getBindingTableGroups().add("t_order");
        Properties properties = new Properties();
        //打开真实SQL打印
        properties.setProperty("sql.show", "true");
        return ShardingDataSourceFactory.createDataSource(createDataSourceMap(), shardingRuleConfig, new HashMap<String, Object>(), properties);
    }

    //配置Order表分片规则
    public TableRuleConfiguration getOrderTableRuleConfiguration() {
        TableRuleConfiguration result = new TableRuleConfiguration();
        result.setLogicTable("t_order");
        //配置分片节点
        result.setActualDataNodes("notify_ds.t_order_${[0, 1]}");
        //配置分片算法
        result.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new ConsistentHashingPreciseShardingAlgorithmSharding()));
        return result;
    }

    public Map<String, DataSource> createDataSourceMap() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        Map<String, DataSource> result = new HashMap<>();
        result.put("notify_ds", dataSource);
        return result;
    }

    @Bean(name = "notifyTransactionManager")
    public DataSourceTransactionManager notifyTransactionManager() throws SQLException {
        return new DataSourceTransactionManager(notifyDataSource());
    }

    @Bean(name = "notifySqlSessionFactory")
    public SqlSessionFactory notifySqlSessionFactory(@Qualifier("notifyDataSource") DataSource notifyDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(notifyDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(NotifyDataSourceConfig.MAPPER_LOCATION));
        return sessionFactory.getObject();
    }

}
