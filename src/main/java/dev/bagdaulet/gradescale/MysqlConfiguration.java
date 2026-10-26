package dev.bagdaulet.gradescale;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("mysql")
@EnableScheduling
@org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession(
    maxInactiveIntervalInSeconds = 86400)
public class MysqlConfiguration {
  @Bean
  DataSource dataSource(
      @Value("${MYSQL_HOST:127.0.0.1}") String host,
      @Value("${MYSQL_PORT:3306}") int port,
      @Value("${MYSQL_DATABASE:grade_scale}") String database,
      @Value("${MYSQL_USER}") String user,
      @Value("${MYSQL_PASSWORD}") String password,
      @Value("${MYSQL_SSL_MODE:REQUIRED}") String sslMode) {
    if (!host.matches("[A-Za-z0-9.-]+")
        || !database.matches("[A-Za-z0-9_]+")
        || port < 1
        || port > 65535) throw new IllegalArgumentException("Invalid database configuration");
    if (!java.util.Set.of("DISABLED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY").contains(sslMode))
      throw new IllegalArgumentException("Invalid SSL mode");
    if (user.equals("root") || password.isBlank())
      throw new IllegalArgumentException("Use a dedicated database user and a non-empty password");
    DriverManagerDataSource ds =
        new DriverManagerDataSource(
            "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + database
                + "?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=10000&sslMode="
                + sslMode,
            user,
            password);
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
    return ds;
  }

  @Bean
  JdbcTemplate jdbcTemplate(DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  org.springframework.transaction.PlatformTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  @Bean
  TransactionTemplate transactions(
      org.springframework.transaction.PlatformTransactionManager manager) {
    return new TransactionTemplate(manager);
  }

  @Bean
  org.springframework.session.web.http.DefaultCookieSerializer cookieSerializer(
      @Value("${SECURE_COOKIES:false}") boolean secure) {
    var cookie = new org.springframework.session.web.http.DefaultCookieSerializer();
    cookie.setCookieName("GRADE_SCALE_SESSION");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setSameSite("Strict");
    cookie.setUseSecureCookie(secure);
    return cookie;
  }
}
