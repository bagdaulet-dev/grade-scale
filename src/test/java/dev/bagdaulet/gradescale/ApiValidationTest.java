package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import org.testng.annotations.*;

public class ApiValidationTest extends ApiTestSupport {
  @DataProvider
  public Object[][] invalidCourses() {
    return new Object[][] {
      {"{\"name\":\"   \",\"code\":\"X\"}", 422},
      {"{\"name\":\"Valid\"}", 422},
      {"{\"name\":\"Valid\",\"code\":\"X\",\"owner\":\"forged\"}", 400},
      {"{\"name\":\"Bad\\u0000name\",\"code\":\"X\"}", 422},
      {"{\"name\":\"Valid\",\"code\":\"Bad\\ncode\"}", 422}
    };
  }

  @Test(dataProvider = "invalidCourses")
  public void validatesCourseBoundaries(String body, int status) throws Exception {
    ok(client(), "POST", "/courses", body, status);
  }

  @Test
  public void failedPatchPreservesAllOriginalFields() throws Exception {
    Client c = client();
    String id = course(c);
    String a = assessment(c, id, "Quiz", 80, 60);
    assessment(c, id, "Exam", 90, 40);
    ok(c, "PATCH", "/assessments/" + a, "{\"name\":\"Corrupted\",\"score\":0,\"weight\":61}", 422);
    var detail = ok(c, "GET", "/courses/" + id, null, 200);
    assertEquals(detail.path("assessments").get(0).path("name").asString(), "Quiz");
    assertEquals(detail.path("assessments").get(0).path("score").asInt(), 80);
    assertEquals(detail.path("result").path("score").asDouble(), 84.0);
  }

  @Test
  public void omittedAndNullPatchFieldsKeepTheirValues() throws Exception {
    Client c = client();
    String id = course(c);
    String a = assessment(c, id, "Quiz", 80, 40);
    var result = ok(c, "PATCH", "/assessments/" + a, "{\"name\":null,\"score\":85}", 200);
    assertEquals(result.path("name").asString(), "Quiz");
    assertEquals(result.path("weight").asInt(), 40);
    assertEquals(result.path("score").asInt(), 85);
  }
}
