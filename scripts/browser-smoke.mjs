import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { chromium } from "playwright";

const browser = await chromium.launch({
  channel: process.env.BROWSER_CHANNEL || undefined,
  headless: true,
});
const context = await browser.newContext({
  viewport: { width: 1440, height: 1000 },
  reducedMotion: "reduce",
});
const page = await context.newPage();
const errors = [];
page.on("pageerror", (error) => errors.push(error.message));
page.on("console", (message) => {
  if (message.type() === "error" && !/\b422\b/.test(message.text()))
    errors.push(message.text());
});
const base = process.env.BASE_URL || "http://localhost:8081/";
const waitText = (selector, expected) =>
  page.locator(selector).filter({ hasText: expected }).waitFor();
try {
  await page.goto(base);
  await page
    .getByRole("button", { name: "Try an example", exact: true })
    .click();
  await waitText("#grade-score", "90.13");
  await page.getByRole("button", { name: "Find out", exact: true }).click();
  await waitText("#target-result", "89.6%");
  await page
    .getByRole("button", { name: "Edit Weekly labs", exact: true })
    .click();
  await page.getByLabel("Score (%)", { exact: true }).fill("100");
  await page
    .getByRole("button", { name: "Save assessment", exact: true })
    .click();
  await waitText("#grade-score", "92.27");
  await page
    .getByRole("button", { name: "Edit Weekly labs", exact: true })
    .click();
  await page.getByLabel("Score (%)", { exact: true }).fill("92");
  await page
    .getByRole("button", { name: "Save assessment", exact: true })
    .click();
  await waitText("#grade-score", "90.13");
  await page
    .getByRole("button", { name: "Add assessment Up to 20", exact: true })
    .click();
  await page.getByLabel("Assessment name", { exact: true }).fill("Final exam");
  await page.getByLabel("Score (%)", { exact: true }).fill("90");
  await page.getByLabel("Weight (%)", { exact: true }).fill("30");
  await page
    .getByRole("button", { name: "Save assessment", exact: true })
    .click();
  await waitText("#assessment-error", "cannot exceed 100%");
  await page.getByLabel("Weight (%)", { exact: true }).fill("25");
  await page
    .getByRole("button", { name: "Save assessment", exact: true })
    .click();
  await waitText("#result-status", "Complete");
  await waitText("#grade-score", "90.1");
  await page
    .getByRole("button", { name: "Remove Final exam", exact: true })
    .click();
  await page.getByRole("button", { name: "Keep it", exact: true }).click();
  assert.equal(
    await page
      .getByRole("button", { name: "Remove Final exam", exact: true })
      .count(),
    1,
  );
  await page
    .getByRole("button", { name: "Remove Final exam", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Remove assessment", exact: true })
    .click();
  await waitText("#result-status", "Provisional");
  await waitText("#grade-score", "90.13");
  await page
    .getByRole("button", { name: "Grading scale", exact: true })
    .click();
  await waitText("#info-title", "A scale, with context.");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Close", exact: true })
    .click();
  await page.keyboard.press("Escape");
  await page.reload();
  await waitText("#grade-score", "90.13");
  await mkdir("docs/screenshots", { recursive: true });
  await page.screenshot({
    path: "docs/screenshots/desktop.png",
    fullPage: true,
  });
  for (const width of [320, 390, 768]) {
    await page.setViewportSize({ width, height: 844 });
    const fits = await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    );
    assert.equal(fits, true, `Horizontal overflow at ${width}px`);
    if (width === 390)
      await page.screenshot({
        path: "docs/screenshots/mobile.png",
        fullPage: true,
      });
  }
  const other = await browser.newContext();
  const privatePage = await other.newPage();
  await privatePage.goto(base);
  await privatePage
    .getByRole("heading", { name: "Start with one course.", exact: true })
    .waitFor();
  await other.close();
  assert.deepEqual(errors, [], `Browser errors: ${errors.join("; ")}`);
  console.log(
    "Browser journey passed: create, edit, overflow validation, target, deletion, reload, isolation, and responsive layouts.",
  );
} finally {
  await context.close();
  await browser.close();
}
