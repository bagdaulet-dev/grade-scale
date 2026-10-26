package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tools.jackson.databind.json.JsonMapper;

public class MysqlPersistenceTest {
  private ConfigurableApplicationContext start() {
    return new SpringApplicationBuilder(GradeScaleApplication.class)
        .profiles("mysql")
        .run("--server.port=0", "--logging.level.root=ERROR", "--debug=false");
  }

  @Test
  public void browserSessionAndCoursesSurviveRestartAndExpire() throws Exception {
    if (!"mysql".equals(System.getenv("TEST_STORAGE")))
      throw new SkipException("MySQL-only persistence check; set TEST_STORAGE=mysql.");
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    JsonMapper json = JsonMapper.builder().build();
    String id;
    try (ConfigurableApplicationContext app = start()) {
      String base =
          "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port") + "/api";
      String session =
          client
              .send(
                  HttpRequest.newBuilder(URI.create(base + "/session")).GET().build(),
                  HttpResponse.BodyHandlers.ofString())
              .body();
      String csrf = json.readTree(session).path("csrfToken").asString();
      HttpResponse<String> response =
          client.send(
              HttpRequest.newBuilder(URI.create(base + "/courses"))
                  .header("Content-Type", "application/json")
                  .header("X-CSRF-TOKEN", csrf)
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "{\"name\":\"Restart test\",\"code\":\"TEST\"}"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(response.statusCode(), 201, response.body());
      id = json.readTree(response.body()).path("course").path("id").asString();
      assertTrue(
          app.getBean(JdbcTemplate.class)
                  .queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)
              > 0);
    }
    try (ConfigurableApplicationContext app = start()) {
      String url =
          "http://127.0.0.1:"
              + app.getEnvironment().getProperty("local.server.port")
              + "/api/courses/"
              + id;
      HttpResponse<String> response =
          client.send(
              HttpRequest.newBuilder(URI.create(url)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(
          response.statusCode(),
          200,
          "Saved session did not survive the app restart: " + response.body());
      JdbcTemplate jdbc = app.getBean(JdbcTemplate.class);
      jdbc.update(
          "UPDATE workspaces SET expires_at=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND WHERE owner=(SELECT owner FROM courses WHERE id=?)",
          id);
      assertEquals(
          client
              .send(
                  HttpRequest.newBuilder(URI.create(url)).GET().build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode(),
          404);
      app.getBean(MysqlCourseRepository.class).cleanup();
      assertEquals(
          jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE id=?", Integer.class, id),
          Integer.valueOf(0));
    }
  }
}
