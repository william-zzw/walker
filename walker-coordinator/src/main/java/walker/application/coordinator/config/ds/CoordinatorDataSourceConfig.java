package walker.application.coordinator.config.ds;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
// 扫描 Mapper 接口并容器管理
@MapperScan(basePackages = CoordinatorDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "coordinatorSqlSessionFactory")
public class CoordinatorDataSourceConfig {

    // 配置使用该数据源的包和sql
    static final String PACKAGE = "walker.application.coordinator.mapper";
    static final String MAPPER_LOCATION = "classpath:mapper/walker/application/coordinator/*.xml";

    @Value("${coordinator.datasource.url}")
    private String url;

    @Value("${coordinator.datasource.username}")
    private String user;

    @Value("${coordinator.datasource.password}")
    private String password;

    @Value("${coordinator.datasource.driverClassName}")
    private String driverClass;

    @Bean(name = "coordinatorDataSource")
    @Primary
    public DataSource coordinatorDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }

    @Bean(name = "coordinatorTransactionManager")
    @Primary
    public DataSourceTransactionManager coordinatorTransactionManager() {
        return new DataSourceTransactionManager(coordinatorDataSource());
    }

    @Bean(name = "coordinatorSqlSessionFactory")
    @Primary
    public SqlSessionFactory coordinatorSqlSessionFactory(@Qualifier("coordinatorDataSource") DataSource coordinatorDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(coordinatorDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(CoordinatorDataSourceConfig.MAPPER_LOCATION));
        return sessionFactory.getObject();
    }

}
