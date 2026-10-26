package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;
import static org.testng.Assert.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.*;

public class GradeCalculatorTest {
  private final GradeCalculator calculator = new GradeCalculator();

  private Assessment assessment(String score, String weight) {
    return new Assessment(UUID.randomUUID(), "Exam", new BigDecimal(score), new BigDecimal(weight));
  }

  @Test
  public void calculatesTheDocumentedExample() {
    GradeResult r = calculator.calculate(List.of(assessment("80", "40"), assessment("90", "60")));
    assertEquals(r.score(), new BigDecimal("86.00"));
    assertEquals(r.letterGrade(), "B+");
    assertFalse(r.provisional());
  }

  @Test
  public void incompleteCourseIsProvisionalAndNormalizesGradedWork() {
    GradeResult r = calculator.calculate(List.of(assessment("80", "40")));
    assertEquals(r.score(), new BigDecimal("80.00"));
    assertEquals(r.earnedPoints(), new BigDecimal("32.00"));
    assertTrue(r.provisional());
    assertEquals(r.remainingWeight(), new BigDecimal("60"));
  }

  @Test
  public void emptyCourseHasNoInventedGrade() {
    GradeResult r = calculator.calculate(List.of());
    assertNull(r.score());
    assertNull(r.letterGrade());
    assertTrue(r.provisional());
  }

  @Test(expectedExceptions = ApiException.class)
  public void rejectsWeightOverflow() {
    calculator.calculate(List.of(assessment("90", "60"), assessment("80", "40.01")));
  }

  @Test
  public void decimalWeightsUseDecimalArithmetic() {
    GradeResult r =
        calculator.calculate(
            List.of(
                assessment("90", "33.33"), assessment("80", "33.33"), assessment("100", "33.34")));
    assertEquals(r.score(), new BigDecimal("90.00"));
    assertFalse(r.provisional());
  }

  @Test
  public void roundsBeforeAssigningTheLetter() {
    GradeResult result =
        calculator.calculate(List.of(assessment("95", "50"), assessment("94.99", "50")));
    assertEquals(result.score(), new BigDecimal("95.00"));
    assertEquals(result.letterGrade(), "A");
  }

  @DataProvider
  public Object[][] boundaries() {
    return new Object[][] {
      {"100", "A"}, {"95", "A"}, {"94.99", "A-"}, {"90", "A-"},
      {"89.99", "B+"}, {"85", "B+"}, {"84.99", "B"}, {"80", "B"},
      {"79.99", "B-"}, {"75", "B-"}, {"74.99", "C+"}, {"70", "C+"},
      {"69.99", "C"}, {"65", "C"}, {"64.99", "C-"}, {"60", "C-"},
      {"59.99", "D+"}, {"55", "D+"}, {"54.99", "D"}, {"50", "D"},
      {"49.99", "F"}, {"0", "F"}
    };
  }

  @Test(dataProvider = "boundaries")
  public void documentsGradeBoundaries(String score, String letter) {
    assertEquals(calculator.letter(new BigDecimal(score)), letter);
  }

  @Test
  public void findsRequiredFinalScore() {
    TargetResult r = calculator.target(List.of(assessment("80", "40")), new BigDecimal("86"));
    assertEquals(r.requiredScore(), new BigDecimal("90.00"));
    assertEquals(r.status(), "REACHABLE");
  }

  @Test
  public void targetRoundsUpSoAdviceReachesTheGoal() {
    TargetResult r = calculator.target(List.of(assessment("50", "30")), new BigDecimal("80"));
    assertEquals(r.requiredScore(), new BigDecimal("92.86"));
  }

  @Test
  public void identifiesUnreachableAndSecuredTargets() {
    assertEquals(
        calculator.target(List.of(assessment("0", "90")), new BigDecimal("90")).status(),
        "UNREACHABLE");
    TargetResult r = calculator.target(List.of(assessment("100", "90")), new BigDecimal("60"));
    assertEquals(r.status(), "SECURED");
    assertEquals(r.requiredScore(), new BigDecimal("0.00"));
  }

  @Test(expectedExceptions = ApiException.class)
  public void targetRequiresRemainingWork() {
    calculator.target(List.of(assessment("100", "100")), new BigDecimal("90"));
  }

  @Test(expectedExceptions = ApiException.class)
  public void invalidTargetIsRejected() {
    calculator.target(List.of(), new BigDecimal("101"));
  }
}
