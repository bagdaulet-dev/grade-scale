package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.*;
import tools.jackson.databind.JsonNode;

public class ApiTest extends ApiTestSupport {
  @Test
  public void publishesTheRequestedScale() throws Exception {
    Client c = client();
    JsonNode scale = ok(c, "GET", "/scale", null, 200);
    assertEquals(scale.size(), 11);
    assertEquals(scale.get(0).path("letter").asString(), "A");
    assertEquals(scale.get(0).path("minimum").asInt(), 95);
    assertEquals(scale.get(1).path("letter").asString(), "A-");
    assertEquals(scale.get(1).path("minimum").asInt(), 90);
    assertEquals(scale.get(2).path("letter").asString(), "B+");
    assertEquals(scale.get(2).path("minimum").asInt(), 85);
    assertEquals(scale.get(10).path("letter").asString(), "F");
  }

  @Test
  public void fullCourseJourneyAndProvisionalResults() throws Exception {
    Client c = client();
    String id = course(c);
    String a = assessment(c, id, "Assignments", 80, 40);
    JsonNode r = ok(c, "GET", "/courses/" + id + "/result", null, 200);
    assertTrue(r.path("provisional").asBoolean());
    assertEquals(r.path("score").asDouble(), 80.0);
    assessment(c, id, "Final", 90, 60);
    r = ok(c, "GET", "/courses/" + id + "/result", null, 200);
    assertEquals(r.path("score").asDouble(), 86.0);
    assertFalse(r.path("provisional").asBoolean());
    ok(c, "PATCH", "/assessments/" + a, "{\"score\":100}", 200);
    assertEquals(
        ok(c, "GET", "/courses/" + id + "/result", null, 200).path("score").asDouble(), 94.0);
    ok(c, "PUT", "/courses/" + id, "{\"name\":\"Renamed\",\"code\":\"CS\"}", 200);
    ok(c, "DELETE", "/assessments/" + a, null, 204);
    assertTrue(
        ok(c, "GET", "/courses/" + id + "/result", null, 200).path("provisional").asBoolean());
    ok(c, "DELETE", "/courses/" + id, null, 204);
    ok(c, "GET", "/courses/" + id, null, 404);
  }

  @Test
  public void foreignSessionsCannotReadOrChangeData() throws Exception {
    Client owner = client(), other = client();
    String id = course(owner);
    String aid = assessment(owner, id, "Quiz", 80, 10);
    ok(other, "GET", "/courses/" + id, null, 404);
    ok(other, "DELETE", "/courses/" + id, null, 404);
    ok(other, "PATCH", "/assessments/" + aid, "{\"score\":100}", 404);
    ok(other, "DELETE", "/assessments/" + aid, null, 404);
    assertEquals(ok(other, "GET", "/courses", null, 200).path("totalItems").asInt(), 0);
  }

  @Test
  public void unsafeRequestsRequireCsrf() throws Exception {
    Client c = client();
    assertEquals(
        send(c, "POST", "/courses", "{\"name\":\"Test\",\"code\":\"\"}", false).statusCode(), 403);
  }

  @Test
  public void validationRejectsBadNumbersAndWeightOverflow() throws Exception {
    Client c = client();
    String id = course(c);
    for (String score : List.of("-1", "101", "80.123", "null", "\"not a number\"")) {
      int status =
          send(
                  c,
                  "POST",
                  "/courses/" + id + "/assessments",
                  "{\"name\":\"Exam\",\"score\":" + score + ",\"weight\":40}",
                  true)
              .statusCode();
      assertTrue(status == 400 || status == 422, "Unexpected validation status: " + status);
    }
    String a = assessment(c, id, "One", 80, 60);
    assessment(c, id, "Two", 90, 40);
    ok(
        c,
        "POST",
        "/courses/" + id + "/assessments",
        "{\"name\":\"Overflow\",\"score\":90,\"weight\":1}",
        422);
    ok(c, "PATCH", "/assessments/" + a, "{\"weight\":61}", 422);
    assertEquals(
        ok(c, "GET", "/courses/" + id + "/result", null, 200).path("completedWeight").asDouble(),
        100.0);
  }

  @Test
  public void rejectsMalformedAndOversizedJson() throws Exception {
    Client c = client();
    ok(c, "POST", "/courses", "not-json", 400);
    ok(c, "POST", "/courses", "{\"name\":\"" + "a".repeat(17000) + "\",\"code\":\"\"}", 413);
    ok(c, "GET", "/courses?page=0", null, 422);
    ok(c, "GET", "/courses/not-a-uuid", null, 400);
  }

  @Test
  public void targetPlannerHasReachableAndImpossibleStates() throws Exception {
    Client c = client();
    String id = course(c);
    assessment(c, id, "Work", 80, 40);
    JsonNode r = ok(c, "GET", "/courses/" + id + "/target?score=86", null, 200);
    assertEquals(r.path("requiredScore").asDouble(), 90.0);
    assertEquals(
        ok(c, "GET", "/courses/" + id + "/target?score=100", null, 200).path("status").asString(),
        "UNREACHABLE");
    ok(c, "GET", "/courses/" + id + "/target?score=101", null, 422);
  }

  @Test
  public void concurrentAddsCannotOverfillTheCourse() throws Exception {
    Client c = client();
    String id = course(c);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> call =
          () ->
              send(
                      c,
                      "POST",
                      "/courses/" + id + "/assessments",
                      "{\"name\":\"Exam\",\"score\":80,\"weight\":60}",
                      true)
                  .statusCode();
      List<Future<Integer>> futures = pool.invokeAll(List.of(call, call));
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> f : futures) statuses.add(f.get());
      Collections.sort(statuses);
      assertEquals(statuses, List.of(201, 422));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void securityHeadersAndLiteralNames() throws Exception {
    Client c = client();
    HttpResponse<String> r = send(c, "GET", "/courses", null, true);
    assertTrue(
        r.headers()
            .firstValue("content-security-policy")
            .orElse("")
            .contains("default-src 'self'"));
    assertEquals(r.headers().firstValue("cache-control").orElse(""), "no-store");
    JsonNode data =
        ok(
            c,
            "POST",
            "/courses",
            "{\"name\":\"<script>alert(1)</script>\",\"code\":\"SQL ' ; --\"}",
            201);
    assertEquals(data.path("course").path("name").asString(), "<script>alert(1)</script>");
  }
}
