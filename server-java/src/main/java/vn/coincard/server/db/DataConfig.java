package vn.coincard.server.db;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** SQLite DataSource + JdbcTemplate. No JPA, no AOP. */
@Configuration
public class DataConfig {
  @Value("${DATABASE_URL:}")
  private String databaseUrl;

  @Value("${coincard.db-url}")
  private String jdbcUrl;

  private String resolvedJdbcUrl() {
    if (databaseUrl == null || databaseUrl.isBlank()) return jdbcUrl;
    String value = databaseUrl.trim();
    return value.startsWith("file:") ? "jdbc:sqlite:" + value.substring(5) : value;
  }

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName("org.sqlite.JDBC");
    ds.setUrl(resolvedJdbcUrl());
    return ds;
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
