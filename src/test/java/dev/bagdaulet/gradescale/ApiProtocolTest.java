package dev.bagdaulet.gradescale;

import static org.testng.Assert.*;

import java.net.URI;
import java.net.http.*;
import org.testng.annotations.Test;

public class ApiProtocolTest extends ApiTestSupport {
  @Test
  public void methodErrorsAdvertiseAllowedMethods() throws Exception {
    Client c = client();
    var response = send(c, "PATCH", "/courses", "{}", true);
    assertEquals(response.statusCode(), 405);
    String allow = response.headers().firstValue("Allow").orElse("");
    assertTrue(
        allow.contains("GET") && allow.contains("POST"), "405 responses need an Allow header");
  }

  @Test
  public void unsupportedRepresentationReturnsAnExplicitError() throws Exception {
    Client c = client();
    var response =
        c.http()
            .send(
                HttpRequest.newBuilder(URI.create(base + "/health"))
                    .header("Accept", "application/xml")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(response.statusCode(), 406);
    assertEquals(
        json.readTree(response.body()).path("error").path("code").asString(), "NOT_ACCEPTABLE");
  }

  @Test
  public void oversizedRequestsStillHaveSecurityHeaders() throws Exception {
    Client c = client();
    var response = send(c, "POST", "/courses", "x".repeat(17000), true);
    assertEquals(response.statusCode(), 413);
    assertEquals(response.headers().firstValue("X-Frame-Options").orElse(""), "DENY");
    assertTrue(
        response
            .headers()
            .firstValue("Content-Security-Policy")
            .orElse("")
            .contains("default-src 'self'"));
    assertEquals(response.headers().firstValue("Cache-Control").orElse(""), "no-store");
  }
}
