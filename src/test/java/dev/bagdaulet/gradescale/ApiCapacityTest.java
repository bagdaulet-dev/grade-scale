package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.Test;

public class ApiCapacityTest extends ApiTestSupport {
  @Test
  public void courseCapacityAndPaginationStayBounded() throws Exception {
    Client c = client();
    for (int i = 0; i < 20; i++) course(c);
    ok(c, "POST", "/courses", "{\"name\":\"Overflow\",\"code\":\"\"}", 422);
    var page = ok(c, "GET", "/courses", null, 200);
    assertEquals(page.path("items").size(), 20);
    assertEquals(page.path("totalItems").asInt(), 20);
    assertEquals(ok(c, "GET", "/courses?page=2", null, 200).path("items").size(), 0);
  }

  @Test
  public void assessmentCapacityRejectsWithoutChangingTheResult() throws Exception {
    Client c = client();
    String id = course(c);
    for (int i = 0; i < 20; i++) assessment(c, id, "Quiz " + i, 80, 1);
    ok(
        c,
        "POST",
        "/courses/" + id + "/assessments",
        "{\"name\":\"Overflow\",\"score\":100,\"weight\":1}",
        422);
    var detail = ok(c, "GET", "/courses/" + id, null, 200);
    assertEquals(detail.path("assessments").size(), 20);
    assertEquals(detail.path("result").path("completedWeight").asInt(), 20);
  }

  @Test
  public void simultaneousCourseCreationHonorsTheWorkspaceLimit() throws Exception {
    Client c = client();
    for (int i = 0; i < 19; i++) course(c);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> create =
          () ->
              send(c, "POST", "/courses", "{\"name\":\"Last place\",\"code\":\"\"}", true)
                  .statusCode();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> f : pool.invokeAll(List.of(create, create))) statuses.add(f.get());
      Collections.sort(statuses);
      assertEquals(statuses, List.of(201, 422));
    } finally {
      pool.shutdownNow();
    }
  }
}
