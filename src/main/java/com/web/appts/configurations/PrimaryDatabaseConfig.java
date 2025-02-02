package com.web.appts.configurations;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EntityScan(basePackages = "com.web.appts.entities")
@EnableJpaRepositories(basePackages = "com.web.appts.repositories")
public class PrimaryDatabaseConfig {

    @Bean(name = "primaryDataSource")
    @Primary
    public DataSource primaryDataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url("jdbc:mysql://localhost:3306/dataprd");
        dataSourceBuilder.username("root");
        dataSourceBuilder.password("123456");
        dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
        return dataSourceBuilder.build();
    }

}