package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ApiAccessControlTest extends ApiTestSupport {
  @Test
  public void everyCourseOperationRejectsForeignOwners() throws Exception {
    Client owner = client(), other = client();
    String id = course(owner);
    ok(other, "GET", "/courses/" + id + "/result", null, 404);
    ok(other, "GET", "/courses/" + id + "/target?score=90", null, 404);
    ok(other, "PUT", "/courses/" + id, "{\"name\":\"Stolen\",\"code\":\"X\"}", 404);
    ok(
        other,
        "POST",
        "/courses/" + id + "/assessments",
        "{\"name\":\"Injected\",\"score\":100,\"weight\":100}",
        404);
    assertEquals(ok(owner, "GET", "/courses/" + id, null, 200).path("assessments").size(), 0);
  }

  @Test
  public void allWriteMethodsRequireTheSessionToken() throws Exception {
    Client c = client();
    String id = course(c);
    String a = assessment(c, id, "Quiz", 80, 10);
    assertEquals(
        send(c, "PUT", "/courses/" + id, "{\"name\":\"Changed\",\"code\":\"X\"}", false)
            .statusCode(),
        403);
    assertEquals(send(c, "PATCH", "/assessments/" + a, "{\"score\":100}", false).statusCode(), 403);
    assertEquals(send(c, "DELETE", "/assessments/" + a, null, false).statusCode(), 403);
    assertEquals(send(c, "DELETE", "/courses/" + id, null, false).statusCode(), 403);
    assertEquals(
        ok(c, "GET", "/courses/" + id + "/result", null, 200).path("score").asDouble(), 80.0);
  }
}
