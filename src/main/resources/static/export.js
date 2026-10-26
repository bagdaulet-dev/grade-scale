/** Download only the course data already visible in the private workspace. */
export function downloadCourse(detail, gradingScale) {
  const data = {
    format: "grade-scale-course",
    version: 1,
    exportedAt: new Date().toISOString(),
    ...detail,
    gradingScale,
  };
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: "application/json;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "grade-scale-course.json";
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
