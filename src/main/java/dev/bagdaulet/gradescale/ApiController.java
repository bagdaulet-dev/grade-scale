package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final CourseRepository repository;
  private final GradeCalculator calculator;

  public ApiController(CourseRepository repository, GradeCalculator calculator) {
    this.repository = repository;
    this.calculator = calculator;
  }

  private String owner(HttpSession session) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(session.getId().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @GetMapping("/scale")
  public java.util.List<GradingScale.Band> scale() {
    return GradingScale.BANDS;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    repository.checkHealth();
    return Map.of("status", "ok");
  }

  @GetMapping("/session")
  public Map<String, String> session(CsrfToken token, HttpSession session) {
    return Map.of(
        "csrfToken",
        token.getToken(),
        "csrfHeader",
        token.getHeaderName(),
        "storage",
        repository.storage());
  }

  @GetMapping("/courses")
  public CoursePage list(HttpSession session, @RequestParam(defaultValue = "1") int page) {
    if (page < 1 || page > 100) throw ApiException.invalid("Page must be from 1 to 100.");
    return repository.list(owner(session), page);
  }

  @PostMapping("/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public CourseDetail create(HttpSession s, @Valid @RequestBody CourseInput input) {
    return repository.create(owner(s), input);
  }

  @GetMapping("/courses/{id}")
  public CourseDetail detail(HttpSession s, @PathVariable UUID id) {
    return repository.detail(owner(s), id);
  }

  @PutMapping("/courses/{id}")
  public CourseDetail update(
      HttpSession s, @PathVariable UUID id, @Valid @RequestBody CourseInput input) {
    return repository.update(owner(s), id, input);
  }

  @DeleteMapping("/courses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(HttpSession s, @PathVariable UUID id) {
    repository.delete(owner(s), id);
  }

  @PostMapping("/courses/{id}/assessments")
  @ResponseStatus(HttpStatus.CREATED)
  public Assessment add(
      HttpSession s, @PathVariable UUID id, @Valid @RequestBody AssessmentInput input) {
    return repository.add(owner(s), id, input);
  }

  @PatchMapping("/assessments/{id}")
  public Assessment patch(
      HttpSession s, @PathVariable UUID id, @Valid @RequestBody AssessmentPatch input) {
    return repository.patch(owner(s), id, input);
  }

  @DeleteMapping("/assessments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(HttpSession s, @PathVariable UUID id) {
    repository.remove(owner(s), id);
  }

  @GetMapping("/courses/{id}/result")
  public GradeResult result(HttpSession s, @PathVariable UUID id) {
    return repository.detail(owner(s), id).result();
  }

  @GetMapping("/courses/{id}/target")
  public TargetResult target(HttpSession s, @PathVariable UUID id, @RequestParam BigDecimal score) {
    return calculator.target(repository.detail(owner(s), id).assessments(), score);
  }
}
