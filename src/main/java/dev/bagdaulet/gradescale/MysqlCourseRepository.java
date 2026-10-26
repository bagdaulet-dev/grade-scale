package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("mysql")
public class MysqlCourseRepository implements CourseRepository {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final GradeCalculator calculator;
  private final RowMapper<Course> courseMapper =
      (rs, n) ->
          new Course(
              UUID.fromString(rs.getString("id")),
              rs.getString("name"),
              rs.getString("code"),
              rs.getTimestamp("created_at").toInstant());
  private final RowMapper<Assessment> assessmentMapper =
      (rs, n) ->
          new Assessment(
              UUID.fromString(rs.getString("id")),
              rs.getString("name"),
              rs.getBigDecimal("score"),
              rs.getBigDecimal("weight"));

  public MysqlCourseRepository(
      JdbcTemplate jdbc, TransactionTemplate transactions, GradeCalculator calculator) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.calculator = calculator;
  }

  private Course own(String owner, UUID id, boolean lock) {
    List<Course> rows =
        jdbc.query(
            "SELECT c.* FROM courses c JOIN workspaces w ON w.owner=c.owner WHERE c.id=? AND c.owner=? AND w.expires_at>UTC_TIMESTAMP(6)"
                + (lock ? " FOR UPDATE" : ""),
            courseMapper,
            id.toString(),
            owner);
    if (rows.isEmpty()) throw ApiException.notFound();
    return rows.getFirst();
  }

  public CoursePage list(String owner, int page) {
    Integer total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM courses c JOIN workspaces w ON w.owner=c.owner WHERE c.owner=? AND w.expires_at>UTC_TIMESTAMP(6)",
            Integer.class,
            owner);
    List<Course> items =
        jdbc.query(
            "SELECT c.* FROM courses c JOIN workspaces w ON w.owner=c.owner WHERE c.owner=? AND w.expires_at>UTC_TIMESTAMP(6) ORDER BY c.created_at DESC,c.id LIMIT 20 OFFSET ?",
            courseMapper,
            owner,
            (page - 1) * 20);
    return new CoursePage(items, page, 20, total == null ? 0 : total);
  }

  public CourseDetail create(String owner, CourseInput input) {
    return transactions.execute(
        tx -> {
          // A duplicate-key update takes an exclusive lock immediately. INSERT IGNORE
          // takes a shared duplicate-key lock, which can deadlock during a later upgrade.
          jdbc.update(
              "INSERT INTO workspaces(owner,expires_at) VALUES(?,?) ON DUPLICATE KEY UPDATE owner=owner",
              owner,
              Timestamp.from(Instant.now().plus(Duration.ofHours(24))));
          jdbc.queryForObject(
              "SELECT owner FROM workspaces WHERE owner=? FOR UPDATE", String.class, owner);
          // Expired anonymous workspaces cannot recover earlier data.
          jdbc.update(
              "DELETE FROM courses WHERE owner=? AND EXISTS (SELECT 1 FROM workspaces WHERE owner=? AND expires_at<=UTC_TIMESTAMP(6))",
              owner,
              owner);
          jdbc.update(
              "UPDATE workspaces SET expires_at=? WHERE owner=? AND expires_at<=UTC_TIMESTAMP(6)",
              Timestamp.from(Instant.now().plus(Duration.ofHours(24))),
              owner);
          if (list(owner, 1).totalItems() >= 20)
            throw ApiException.invalid(
                "Keep up to 20 courses. Delete a course before adding another.");
          UUID id = UUID.randomUUID();
          jdbc.update(
              "INSERT INTO courses(id,owner,name,code,created_at) VALUES(?,?,?,?,?)",
              id.toString(),
              owner,
              input.name().strip(),
              input.code().strip(),
              Timestamp.from(Instant.now()));
          return detail(owner, id);
        });
  }

  public CourseDetail detail(String owner, UUID id) {
    Course c = own(owner, id, false);
    List<Assessment> assessments = assessments(id);
    return new CourseDetail(c, assessments, calculator.calculate(assessments));
  }

  private List<Assessment> assessments(UUID id) {
    return jdbc.query(
        "SELECT * FROM assessments WHERE course_id=? ORDER BY created_at,id",
        assessmentMapper,
        id.toString());
  }

  public CourseDetail update(String owner, UUID id, CourseInput input) {
    return transactions.execute(
        tx -> {
          own(owner, id, true);
          jdbc.update(
              "UPDATE courses SET name=?,code=? WHERE id=?",
              input.name().strip(),
              input.code().strip(),
              id.toString());
          return detail(owner, id);
        });
  }

  public void delete(String owner, UUID id) {
    transactions.executeWithoutResult(
        tx -> {
          own(owner, id, true);
          jdbc.update("DELETE FROM courses WHERE id=?", id.toString());
        });
  }

  public Assessment add(String owner, UUID id, AssessmentInput input) {
    return transactions.execute(
        tx -> {
          own(owner, id, true);
          List<Assessment> next = new ArrayList<>(assessments(id));
          if (next.size() >= 20)
            throw ApiException.invalid("A course can contain up to 20 assessments.");
          Assessment a =
              new Assessment(
                  UUID.randomUUID(), input.name().strip(), input.score(), input.weight());
          next.add(a);
          calculator.calculate(next);
          jdbc.update(
              "INSERT INTO assessments(id,course_id,name,score,weight) VALUES(?,?,?,?,?)",
              a.id().toString(),
              id.toString(),
              a.name(),
              a.score(),
              a.weight());
          return a;
        });
  }

  private UUID courseFor(String owner, UUID assessmentId) {
    List<String> ids =
        jdbc.queryForList(
            "SELECT a.course_id FROM assessments a JOIN courses c ON c.id=a.course_id JOIN workspaces w ON w.owner=c.owner WHERE a.id=? AND c.owner=? AND w.expires_at>UTC_TIMESTAMP(6)",
            String.class,
            assessmentId.toString(),
            owner);
    if (ids.isEmpty()) throw ApiException.notFound();
    return UUID.fromString(ids.getFirst());
  }

  public Assessment patch(String owner, UUID id, AssessmentPatch input) {
    return transactions.execute(
        tx -> {
          UUID courseId = courseFor(owner, id);
          own(owner, courseId, true);
          List<Assessment> next = new ArrayList<>(assessments(courseId));
          Assessment old =
              next.stream()
                  .filter(a -> a.id().equals(id))
                  .findFirst()
                  .orElseThrow(ApiException::notFound);
          if (input.name() != null && input.name().isBlank())
            throw ApiException.invalid("Give the assessment a name.");
          Assessment a =
              new Assessment(
                  id,
                  input.name() == null ? old.name() : input.name().strip(),
                  input.score() == null ? old.score() : input.score(),
                  input.weight() == null ? old.weight() : input.weight());
          next.set(next.indexOf(old), a);
          calculator.calculate(next);
          jdbc.update(
              "UPDATE assessments SET name=?,score=?,weight=? WHERE id=?",
              a.name(),
              a.score(),
              a.weight(),
              id.toString());
          return a;
        });
  }

  public void remove(String owner, UUID id) {
    transactions.executeWithoutResult(
        tx -> {
          UUID courseId = courseFor(owner, id);
          own(owner, courseId, true);
          jdbc.update("DELETE FROM assessments WHERE id=?", id.toString());
        });
  }

  @Scheduled(fixedDelay = 3600000)
  public void cleanup() {
    jdbc.update("DELETE FROM workspaces WHERE expires_at<=UTC_TIMESTAMP(6)");
  }

  public void checkHealth() {
    jdbc.queryForObject("SELECT 1", Integer.class);
  }

  public String storage() {
    return "mysql";
  }
}
