package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("preview")
public class PreviewCourseRepository implements CourseRepository {
  private record Entry(String owner, Course course, List<Assessment> assessments) {}

  private final Map<UUID, Entry> entries = new LinkedHashMap<>();
  private final GradeCalculator calculator;

  public PreviewCourseRepository(GradeCalculator calculator) {
    this.calculator = calculator;
  }

  private void cleanup() {
    Instant cutoff = Instant.now().minus(Duration.ofHours(24));
    entries.values().removeIf(e -> e.course().createdAt().isBefore(cutoff));
  }

  private Entry own(String owner, UUID id) {
    cleanup();
    Entry e = entries.get(id);
    if (e == null || !e.owner().equals(owner)) throw ApiException.notFound();
    return e;
  }

  public synchronized CoursePage list(String owner, int page) {
    cleanup();
    List<Course> all =
        entries.values().stream()
            .filter(e -> e.owner().equals(owner))
            .map(Entry::course)
            .sorted(Comparator.comparing(Course::createdAt).reversed())
            .toList();
    return new CoursePage(
        all.stream().skip((long) (page - 1) * 20).limit(20).toList(), page, 20, all.size());
  }

  public synchronized CourseDetail create(String owner, CourseInput input) {
    cleanup();
    if (list(owner, 1).totalItems() >= 20)
      throw ApiException.invalid("Keep up to 20 courses. Delete a course before adding another.");
    if (entries.size() >= 2000)
      throw new ApiException(503, "PREVIEW_FULL", "The preview is full. Please try again later.");
    Course c =
        new Course(UUID.randomUUID(), input.name().strip(), input.code().strip(), Instant.now());
    entries.put(c.id(), new Entry(owner, c, new ArrayList<>()));
    return detail(owner, c.id());
  }

  public synchronized CourseDetail detail(String owner, UUID id) {
    Entry e = own(owner, id);
    List<Assessment> a = List.copyOf(e.assessments());
    return new CourseDetail(e.course(), a, calculator.calculate(a));
  }

  public synchronized CourseDetail update(String owner, UUID id, CourseInput input) {
    Entry e = own(owner, id);
    Course c = new Course(id, input.name().strip(), input.code().strip(), e.course().createdAt());
    entries.put(id, new Entry(owner, c, e.assessments()));
    return detail(owner, id);
  }

  public synchronized void delete(String owner, UUID id) {
    own(owner, id);
    entries.remove(id);
  }

  public synchronized Assessment add(String owner, UUID id, AssessmentInput input) {
    Entry e = own(owner, id);
    if (e.assessments().size() >= 20)
      throw ApiException.invalid("A course can contain up to 20 assessments.");
    Assessment a =
        new Assessment(UUID.randomUUID(), input.name().strip(), input.score(), input.weight());
    List<Assessment> next = new ArrayList<>(e.assessments());
    next.add(a);
    calculator.calculate(next);
    e.assessments().add(a);
    return a;
  }

  private Entry find(String owner, UUID id) {
    cleanup();
    return entries.values().stream()
        .filter(
            e ->
                e.owner().equals(owner)
                    && e.assessments().stream().anyMatch(a -> a.id().equals(id)))
        .findFirst()
        .orElseThrow(ApiException::notFound);
  }

  public synchronized Assessment patch(String owner, UUID id, AssessmentPatch input) {
    Entry e = find(owner, id);
    Assessment old =
        e.assessments().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    if (input.name() != null && input.name().isBlank())
      throw ApiException.invalid("Give the assessment a name.");
    Assessment a =
        new Assessment(
            id,
            input.name() == null ? old.name() : input.name().strip(),
            input.score() == null ? old.score() : input.score(),
            input.weight() == null ? old.weight() : input.weight());
    List<Assessment> next = new ArrayList<>(e.assessments());
    int index = next.indexOf(old);
    next.set(index, a);
    calculator.calculate(next);
    e.assessments().set(index, a);
    return a;
  }

  public synchronized void remove(String owner, UUID id) {
    Entry e = find(owner, id);
    e.assessments().removeIf(a -> a.id().equals(id));
  }

  public void checkHealth() {}

  public String storage() {
    return "preview";
  }
}
