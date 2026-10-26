package dev.bagdaulet.gradescale;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Models {
  private Models() {}

  public record Course(UUID id, String name, String code, Instant createdAt) {}

  public record CourseInput(
      @NotBlank @Size(max = 80) String name, @NotNull @Size(max = 20) String code) {}

  public record Assessment(UUID id, String name, BigDecimal score, BigDecimal weight) {}

  public record AssessmentInput(
      @NotBlank @Size(max = 60) String name,
      @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
          BigDecimal score,
      @NotNull
          @DecimalMin(value = "0", inclusive = false)
          @DecimalMax("100")
          @Digits(integer = 3, fraction = 2)
          BigDecimal weight) {}

  public record AssessmentPatch(
      @Size(min = 1, max = 60) String name,
      @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal score,
      @DecimalMin(value = "0", inclusive = false)
          @DecimalMax("100")
          @Digits(integer = 3, fraction = 2)
          BigDecimal weight) {}

  public record Contribution(
      UUID id, String name, BigDecimal score, BigDecimal weight, BigDecimal points) {}

  public record GradeResult(
      BigDecimal score,
      String letterGrade,
      BigDecimal completedWeight,
      BigDecimal remainingWeight,
      BigDecimal earnedPoints,
      boolean provisional,
      List<Contribution> contributions) {}

  public record CourseDetail(Course course, List<Assessment> assessments, GradeResult result) {}

  public record TargetResult(
      BigDecimal target, BigDecimal requiredScore, BigDecimal remainingWeight, String status) {}

  public record CoursePage(List<Course> items, int page, int pageSize, int totalItems) {}
}
