package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GradeCalculator {
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  public GradeResult calculate(List<Assessment> assessments) {
    BigDecimal weight = BigDecimal.ZERO, earned = BigDecimal.ZERO;
    for (Assessment a : assessments) {
      weight = weight.add(a.weight());
      earned = earned.add(a.score().multiply(a.weight()).divide(HUNDRED));
    }
    if (weight.compareTo(HUNDRED) > 0)
      throw ApiException.invalid("Assessment weights cannot exceed 100% in total.");
    BigDecimal score =
        weight.signum() == 0
            ? null
            : earned.multiply(HUNDRED).divide(weight, 2, RoundingMode.HALF_UP);
    return new GradeResult(
        score,
        score == null ? null : letter(score),
        weight,
        HUNDRED.subtract(weight),
        earned.setScale(2, RoundingMode.HALF_UP),
        weight.compareTo(HUNDRED) < 0,
        assessments.stream()
            .map(
                a ->
                    new Contribution(
                        a.id(),
                        a.name(),
                        a.score(),
                        a.weight(),
                        a.score()
                            .multiply(a.weight())
                            .divide(HUNDRED)
                            .setScale(2, RoundingMode.HALF_UP)))
            .toList());
  }

  public TargetResult target(List<Assessment> assessments, BigDecimal target) {
    if (target == null
        || target.signum() < 0
        || target.compareTo(HUNDRED) > 0
        || target.scale() > 2)
      throw ApiException.invalid("Choose a target from 0 to 100 with at most two decimal places.");
    GradeResult result = calculate(assessments);
    if (result.remainingWeight().signum() == 0)
      throw ApiException.invalid(
          "All assessments are complete. Remove an ungraded assessment to plan a final score.");
    BigDecimal earned =
        assessments.stream()
            .map(a -> a.score().multiply(a.weight()).divide(HUNDRED))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal raw =
        target
            .subtract(earned)
            .multiply(HUNDRED)
            .divide(result.remainingWeight(), 10, RoundingMode.HALF_UP);
    String status =
        raw.signum() <= 0 ? "SECURED" : raw.compareTo(HUNDRED) > 0 ? "UNREACHABLE" : "REACHABLE";
    return new TargetResult(
        target,
        raw.max(BigDecimal.ZERO).setScale(2, RoundingMode.CEILING),
        result.remainingWeight(),
        status);
  }

  public String letter(BigDecimal score) {
    return GradingScale.BANDS.stream()
        .filter(band -> score.compareTo(band.minimum()) >= 0)
        .findFirst()
        .orElseThrow()
        .letter();
  }
}
