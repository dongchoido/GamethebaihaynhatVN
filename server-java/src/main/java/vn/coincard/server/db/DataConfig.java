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

  @Value("${coincard.db-url:jdbc:sqlite:../data/coincard.db}")
  private String fallbackDbUrl;

  private String jdbcUrl() {
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      // Reuse server/.env DATABASE_URL ("file:../data/coincard.db" Prisma format).
      String v = databaseUrl.trim();
      if (v.startsWith("file:")) return "jdbc:sqlite:" + v.substring("file:".length());
      return v;
    }
    return fallbackDbUrl;
  }

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName("org.sqlite.JDBC");
    ds.setUrl(jdbcUrl());
    return ds;
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
