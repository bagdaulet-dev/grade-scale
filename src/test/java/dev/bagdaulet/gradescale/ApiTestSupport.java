package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP test fixture shared by focused API behavior tests. */
public abstract class ApiTestSupport {
  protected ConfigurableApplicationContext app;
  protected String base;
  protected final List<HttpClient> clients = new ArrayList<>();
  protected final JsonMapper json = JsonMapper.builder().build();

  protected record Client(HttpClient http, String token) {}

  @BeforeClass
  public void start() {
    app =
        new SpringApplicationBuilder(GradeScaleApplication.class)
            .profiles(System.getenv().getOrDefault("TEST_STORAGE", "preview"))
            .run("--server.port=0", "--logging.level.root=ERROR", "--debug=false");
    base = "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port") + "/api";
  }

  @AfterClass(alwaysRun = true)
  public void stop() {
    clients.forEach(HttpClient::close);
    if (app != null) app.close();
  }

  protected Client client() throws Exception {
    HttpClient http =
        HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    clients.add(http);
    HttpResponse<String> res =
        http.send(
            HttpRequest.newBuilder(URI.create(base + "/session"))
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(res.statusCode(), 200);
    String cookie = res.headers().firstValue("set-cookie").orElse("");
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    return new Client(http, json.readTree(res.body()).path("csrfToken").asString());
  }

  protected HttpResponse<String> send(
      Client c, String method, String path, String body, boolean csrf) throws Exception {
    HttpRequest.Builder req =
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/json")
            .timeout(java.time.Duration.ofSeconds(15));
    if (csrf) req.header("X-CSRF-TOKEN", c.token());
    return c.http()
        .send(
            req.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  protected JsonNode ok(Client c, String method, String path, String body, int status)
      throws Exception {
    HttpResponse<String> r = send(c, method, path, body, true);
    assertEquals(r.statusCode(), status, r.body());
    return status == 204 ? null : json.readTree(r.body());
  }

  protected String course(Client c) throws Exception {
    return ok(c, "POST", "/courses", "{\"name\":\"Software Engineering\",\"code\":\"CS240\"}", 201)
        .path("course")
        .path("id")
        .asString();
  }

  protected String assessment(Client c, String id, String name, int score, int weight)
      throws Exception {
    return ok(
            c,
            "POST",
            "/courses/" + id + "/assessments",
            "{\"name\":\"" + name + "\",\"score\":" + score + ",\"weight\":" + weight + "}",
            201)
        .path("id")
        .asString();
  }
}
