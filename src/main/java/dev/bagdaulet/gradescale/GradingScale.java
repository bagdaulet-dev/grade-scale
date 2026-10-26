package dev.bagdaulet.gradescale;

import java.math.BigDecimal;
import java.util.List;

/** Lower bounds are inclusive. The next higher band's lower bound is exclusive. */
public final class GradingScale {
  public record Band(String letter, BigDecimal minimum) {}

  public static final List<Band> BANDS =
      List.of(
          new Band("A", new BigDecimal("95")),
          new Band("A-", new BigDecimal("90")),
          new Band("B+", new BigDecimal("85")),
          new Band("B", new BigDecimal("80")),
          new Band("B-", new BigDecimal("75")),
          new Band("C+", new BigDecimal("70")),
          new Band("C", new BigDecimal("65")),
          new Band("C-", new BigDecimal("60")),
          new Band("D+", new BigDecimal("55")),
          new Band("D", new BigDecimal("50")),
          new Band("F", BigDecimal.ZERO));

  private GradingScale() {}
}
