package com.web.appts.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class SecondaryDatabaseConfig {

    @Bean(name = "secondaryDataSource")
    public DataSource secondaryDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("sun.jdbc.odbc.JdbcOdbcDriver");
        dataSource.setUrl("jdbc:odbc:DRIVER={Progress OpenEdge 11.7 driver};DSN=AGRPROD2;UID=ODBC;PWD=ODBC;HOST=W2K16DMBBU4;PORT=12501;DB=data;FetchSize=100;PacketSize=4096;");
        dataSource.setUsername("ODBC");
        dataSource.setPassword("ODBC");
        return dataSource;
    }


//        dataSource.setUrl("jdbc:odbc:DRIVER={Progress OpenEdge 11.7 driver};DSN=AGRPROD2;UID=ODBC;PWD=ODBC;HOST=W2K16DMBBU4;PORT=12501;DB=data;");


}
