"use strict";
const $ = (selector) => document.querySelector(selector);
const state = {
  courses: [],
  scale: [],
  detail: null,
  session: null,
  editingCourse: false,
  editingAssessment: null,
  pendingDelete: null,
  busy: false,
  refreshId: 0,
};
let toastTimer;
const number = (value) =>
  value == null
    ? "—"
    : new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(
        value,
      );
function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
function icon(name) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.classList.add("icon");
  svg.setAttribute("aria-hidden", "true");
  const use = document.createElementNS(svg.namespaceURI, "use");
  use.setAttribute("href", `#i-${name}`);
  svg.append(use);
  return svg;
}
function notify(message) {
  clearTimeout(toastTimer);
  $("#toast span").textContent = message;
  $("#toast").hidden = false;
  toastTimer = setTimeout(() => {
    $("#toast").hidden = true;
  }, 3200);
}
function showError(node, message) {
  node.textContent = message;
  node.hidden = !message;
}
async function api(path, options = {}) {
  const method = options.method || "GET";
  const headers = { Accept: "application/json", ...options.headers };
  if (options.body) headers["Content-Type"] = "application/json";
  if (!["GET", "HEAD"].includes(method) && state.session)
    headers[state.session.csrfHeader] = state.session.csrfToken;
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers,
      credentials: "same-origin",
      signal: AbortSignal.timeout(15000),
    });
  } catch (error) {
    if (error.name === "TimeoutError" || error.name === "AbortError")
      throw new Error(
        "The request timed out. Refresh the course before retrying a change.",
      );
    throw new Error(
      "Cannot connect to Grade_scale. Check that the app is running, then refresh.",
    );
  }
  const data =
    response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok)
    throw new Error(
      data?.error?.message ||
        "The request could not be completed. Please try again.",
    );
  return data;
}
async function action(work, errorNode = $("#app-error")) {
  if (state.busy) return;
  state.busy = true;
  const form = errorNode.closest("form");
  const submit = form?.querySelector('button[type="submit"]');
  const submitLabel = submit?.textContent;
  if (submit)
    submit.textContent = form.id === "target-form" ? "Calculating…" : "Saving…";
  if (form) form.setAttribute("aria-busy", "true");
  const buttons = [...document.querySelectorAll("button")].filter(
    (button) => !button.disabled,
  );
  buttons.forEach((button) => {
    button.disabled = true;
  });
  showError(errorNode, "");
  try {
    await work();
  } catch (error) {
    showError(errorNode, error.message);
    errorNode.tabIndex = -1;
    errorNode.focus();
  } finally {
    buttons.forEach((button) => {
      button.disabled = false;
    });
    if (submit) submit.textContent = submitLabel;
    if (form) form.removeAttribute("aria-busy");
    state.busy = false;
  }
}
async function refresh(selectedId = state.detail?.course.id) {
  const refreshId = ++state.refreshId;
  const page = await api("/courses");
  if (refreshId !== state.refreshId) return;
  state.courses = page.items;
  const id = state.courses.some((course) => course.id === selectedId)
    ? selectedId
    : state.courses[0]?.id;
  const detail = id ? await api(`/courses/${id}`) : null;
  if (refreshId !== state.refreshId) return;
  state.detail = detail;
  render();
}
function render() {
  $("#course-count").textContent = state.courses.length;
  $("#empty-state").hidden = !!state.detail;
  $("#course-workspace").hidden = !state.detail;
  const nav = $("#course-nav");
  nav.replaceChildren();
  if (!state.courses.length)
    nav.append(element("p", "sidebar-hint", "Your courses will appear here."));
  state.courses.forEach((course) => {
    const button = element(
      "button",
      `course-nav-item${course.id === state.detail?.course.id ? " selected" : ""}`,
    );
    button.type = "button";
    button.append(
      element("span", "course-dot"),
      element("span", "", course.code || course.name),
    );
    button.setAttribute("title", course.name);
    button.setAttribute("aria-label", `Open ${course.name}`);
    if (course.id === state.detail?.course.id)
      button.setAttribute("aria-current", "page");
    button.addEventListener("click", () => action(() => refresh(course.id)));
    nav.append(button);
  });
  $("#breadcrumb-course").textContent = state.detail?.course.name || "Overview";
  if (!state.detail) return;
  const { course, assessments, result } = state.detail;
  $("#course-title").textContent = course.name;
  $("#course-code").textContent = course.code || "Your course";
  $("#assessment-count").textContent =
    `${assessments.length} assessment${assessments.length === 1 ? "" : "s"}`;
  $("#no-assessments").hidden = assessments.length > 0;
  const rows = $("#assessment-rows");
  rows.replaceChildren();
  assessments.forEach((assessment) => {
    const row = element("tr");
    const nameCell = element("td");
    const name = element("div", "assessment-name");
    const marker = element("span", "assessment-marker");
    marker.append(icon("book"));
    name.append(marker, element("span", "", assessment.name));
    nameCell.append(name);
    const scoreCell = element("td");
    scoreCell.append(
      element("span", "score-value", number(assessment.score)),
      element("span", "score-unit", "%"),
    );
    const weightCell = element("td");
    weightCell.append(
      element("span", "weight-value", `${number(assessment.weight)}%`),
    );
    const actions = element("td");
    const controls = element("div", "row-actions");
    const edit = element("button", "icon-button");
    edit.type = "button";
    edit.setAttribute("aria-label", `Edit ${assessment.name}`);
    edit.append(icon("edit"));
    edit.addEventListener("click", () => assessmentDialog(assessment));
    const remove = element("button", "icon-button");
    remove.type = "button";
    remove.setAttribute("aria-label", `Remove ${assessment.name}`);
    remove.append(icon("trash"));
    remove.addEventListener("click", () =>
      confirmDelete("assessment", assessment),
    );
    controls.append(edit, remove);
    actions.append(controls);
    row.append(nameCell, scoreCell, weightCell, actions);
    rows.append(row);
  });
  $("#weight-total").textContent = `${number(result.completedWeight)} / 100%`;
  $("#remaining-label").textContent = result.remainingWeight
    ? `${number(result.remainingWeight)}% still to come`
    : "All accounted for";
  const track = $("#weight-track");
  track.replaceChildren();
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 100 4");
  svg.setAttribute("width", "100%");
  svg.setAttribute("height", "8");
  svg.setAttribute("preserveAspectRatio", "none");
  let position = 0;
  const colors = ["#b19ac5", "#e8b298", "#aec6b1"];
  assessments.forEach((assessment, index) => {
    const rect = document.createElementNS(svg.namespaceURI, "rect");
    rect.setAttribute("x", position);
    rect.setAttribute("y", "0");
    rect.setAttribute("width", Math.max(0, assessment.weight - 0.5));
    rect.setAttribute("height", "4");
    rect.setAttribute("rx", ".5");
    rect.setAttribute("fill", colors[index % colors.length]);
    svg.append(rect);
    position += Number(assessment.weight);
  });
  track.append(svg);
  track.setAttribute(
    "aria-label",
    `${number(result.completedWeight)} percent graded, ${number(result.remainingWeight)} percent remaining`,
  );
  $("#grade-letter").textContent = result.letterGrade || "—";
  $("#grade-score").textContent = number(result.score);
  $("#grade-caption").textContent =
    result.score == null
      ? "Awaiting your first result"
      : result.provisional
        ? "Average on graded work"
        : "Your completed course grade";
  $("#result-status").textContent = result.provisional
    ? "Provisional"
    : "Complete";
  $("#dial-value").setAttribute("stroke-dasharray", `${result.score || 0} 100`);
  $("#earned-points").replaceChildren(
    document.createTextNode(`${number(result.earnedPoints)} `),
    element("small", "", "pts"),
  );
  $("#remaining-points").replaceChildren(
    document.createTextNode(`${number(result.remainingWeight)} `),
    element("small", "", "%"),
  );
  $("#provisional-note").textContent = result.provisional
    ? "Your current average uses graded work only."
    : "All assessment weights total 100%. This is your final grade.";
  $("#formula-note").textContent = result.provisional
    ? `Weighted points ÷ ${number(result.completedWeight) || "graded"}% completed. Your final grade may change.`
    : "Each score × its weight, added together. Your complete picture.";
  $("#target-result").hidden = true;
}
function courseDialog(editing = false) {
  state.editingCourse = editing;
  $("#course-form").reset();
  showError($("#course-error"), "");
  $("#course-dialog-title").textContent = editing
    ? "Make it yours."
    : "A new beginning.";
  $('#course-form button[type="submit"]').textContent = editing
    ? "Save changes"
    : "Create course";
  if (editing) {
    $("#course-name-input").value = state.detail.course.name;
    $("#course-code-input").value = state.detail.course.code;
  }
  $("#course-dialog").showModal();
  $("#course-name-input").focus();
}
function assessmentDialog(assessment = null) {
  state.editingAssessment = assessment;
  $("#assessment-form").reset();
  showError($("#assessment-error"), "");
  $("#assessment-dialog-title").textContent = assessment
    ? "A little adjustment."
    : "Add an assessment.";
  if (assessment) {
    $("#assessment-name").value = assessment.name;
    $("#assessment-score").value = assessment.score;
    $("#assessment-weight").value = assessment.weight;
  }
  const available =
    state.detail.result.remainingWeight + Number(assessment?.weight || 0);
  $("#assessment-hint").textContent =
    `${number(available)}% weight available. Enter your percentage score, not raw marks.`;
  $("#assessment-dialog").showModal();
  $("#assessment-name").focus();
}
function confirmDelete(kind, item) {
  state.pendingDelete = { kind, item };
  $("#confirm-heading").textContent =
    kind === "course" ? "Delete this course?" : "Remove this assessment?";
  $("#confirm-description").textContent =
    kind === "course"
      ? `“${item.name}” and all its assessments will be deleted. This cannot be undone.`
      : `Remove “${item.name}”? Your grade will be recalculated. This cannot be undone.`;
  $("#confirm-delete").textContent =
    kind === "course" ? "Delete course" : "Remove assessment";
  showError($("#confirm-error"), "");
  $("#confirm-dialog").showModal();
  $("#confirm-dialog .confirm-actions .close-dialog").focus();
}
function info(kind) {
  $("#info-title").textContent =
    kind === "scale"
      ? "A scale, with context."
      : "A small API. A clear contract.";
  const content = $("#info-content");
  content.replaceChildren();
  if (kind === "scale") {
    content.append(
      element(
        "p",
        "",
        "This project uses a simple reference scale. Your school or course may use different boundaries, so check your syllabus.",
      ),
    );
    const table = element("table");
    const header = element("tr");
    ["Letter", "Percentage"].forEach((text) =>
      header.append(element("th", "", text)),
    );
    table.append(header);
    state.scale
      .map((band, index) => [
        band.letter,
        index === 0
          ? `${band.minimum}–100`
          : `${band.minimum}–${number(state.scale[index - 1].minimum - 0.01)}`,
      ])
      .forEach((values) => {
        const row = element("tr");
        values.forEach((text) => row.append(element("td", "", text)));
        table.append(row);
      });
    content.append(
      table,
      element("h3", "", "Weighted, not guessed."),
      element(
        "p",
        "",
        "Weighted points = score × weight ÷ 100. While work remains, the current average is earned points ÷ completed weight × 100. It is provisional until weights total 100%.",
      ),
      element(
        "p",
        "",
        "Example: assignments at 80% with 40% weight, and a final at 90% with 60% weight, give 86% overall: a B+. Results round to two decimals before assigning a letter.",
      ),
    );
  } else {
    content.append(
      element(
        "p",
        "",
        "The Java backend validates scores, stores courses, and calculates every result. The interface talks to these REST endpoints:",
      ),
      element(
        "pre",
        "",
        "GET    /api/session\nGET    /api/courses\nPOST   /api/courses\nGET    /api/courses/{id}\nPUT    /api/courses/{id}\nDELETE /api/courses/{id}\nPOST   /api/courses/{id}/assessments\nPATCH  /api/assessments/{id}\nDELETE /api/assessments/{id}\nGET    /api/courses/{id}/result\nGET    /api/courses/{id}/target?score=90",
      ),
      element(
        "p",
        "",
        "Changes require the session’s CSRF token. Course data is private to the browser session and expires after 24 hours. Full request examples are in docs/api.md.",
      ),
    );
  }
  $("#info-dialog").showModal();
}
["#new-course", "#new-course-side", "#first-course"].forEach((selector) =>
  $(selector).addEventListener("click", () => courseDialog()),
);
$("#rename-course").addEventListener("click", () => courseDialog(true));
$("#delete-course").addEventListener("click", () =>
  confirmDelete("course", state.detail.course),
);
$("#add-assessment").addEventListener("click", () => assessmentDialog());
["#open-scale", "#scale-details"].forEach((selector) =>
  $(selector).addEventListener("click", () => info("scale")),
);
$("#open-api").addEventListener("click", () => info("api"));
document
  .querySelectorAll(".close-dialog")
  .forEach((button) =>
    button.addEventListener("click", () => button.closest("dialog").close()),
  );
document.querySelectorAll("dialog").forEach((dialog) => {
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
      const rect = dialog.getBoundingClientRect();
      if (
        event.clientX < rect.left ||
        event.clientX > rect.right ||
        event.clientY < rect.top ||
        event.clientY > rect.bottom
      )
        dialog.close();
    }
  });
});
$("#course-form").addEventListener("submit", (event) => {
  event.preventDefault();
  action(async () => {
    const name = $("#course-name-input").value.trim();
    if (!name) throw new Error("Give your course a name.");
    const body = JSON.stringify({
      name,
      code: $("#course-code-input").value.trim(),
    });
    const detail = await api(
      state.editingCourse ? `/courses/${state.detail.course.id}` : "/courses",
      { method: state.editingCourse ? "PUT" : "POST", body },
    );
    await refresh(detail.course.id);
    $("#course-dialog").close();
    notify(state.editingCourse ? "Course updated." : "Your course is ready.");
  }, $("#course-error"));
});
$("#assessment-form").addEventListener("submit", (event) => {
  event.preventDefault();
  action(async () => {
    const name = $("#assessment-name").value.trim();
    if (!name) throw new Error("Give your assessment a name.");
    const body = JSON.stringify({
      name,
      score: Number($("#assessment-score").value),
      weight: Number($("#assessment-weight").value),
    });
    await api(
      state.editingAssessment
        ? `/assessments/${state.editingAssessment.id}`
        : `/courses/${state.detail.course.id}/assessments`,
      { method: state.editingAssessment ? "PATCH" : "POST", body },
    );
    await refresh();
    $("#assessment-dialog").close();
    notify(
      state.editingAssessment
        ? "Assessment updated. Grade recalculated."
        : "Assessment saved. Grade recalculated.",
    );
  }, $("#assessment-error"));
});
$("#confirm-delete").addEventListener("click", () =>
  action(async () => {
    const { kind, item } = state.pendingDelete;
    await api(`/${kind === "course" ? "courses" : "assessments"}/${item.id}`, {
      method: "DELETE",
    });
    await refresh();
    $("#confirm-dialog").close();
    notify(
      kind === "course"
        ? "Course deleted."
        : "Assessment removed. Grade recalculated.",
    );
  }, $("#confirm-error")),
);
$("#target-form").addEventListener("submit", (event) => {
  event.preventDefault();
  action(async () => {
    const result = await api(
      `/courses/${state.detail.course.id}/target?score=${encodeURIComponent($("#target-score").value)}`,
    );
    let message;
    if (result.status === "SECURED")
      message = `You’ve already secured ${number(result.target)}% overall, even with zero on the remaining work.`;
    else if (result.status === "UNREACHABLE")
      message = `You would need ${number(result.requiredScore)}% on the remaining ${number(result.remainingWeight)}%. This target is above the possible maximum.`;
    else
      message = `Aim for at least ${number(result.requiredScore)}% across the remaining ${number(result.remainingWeight)}% of your course to finish at ${number(result.target)}%.`;
    $("#target-result").textContent = message;
    $("#target-result").hidden = false;
  }, $("#target-result"));
});
$("#load-example").addEventListener("click", () =>
  action(async () => {
    const detail = await api("/courses", {
      method: "POST",
      body: JSON.stringify({ name: "Software Engineering", code: "CS 240" }),
    });
    // A partial failure keeps the course visible and does not retry writes.
    try {
      for (const assessment of [
        { name: "Weekly labs", score: 92, weight: 20 },
        { name: "Midterm exam", score: 84, weight: 30 },
        { name: "Team project", score: 96, weight: 25 },
      ]) {
        await api(`/courses/${detail.course.id}/assessments`, {
          method: "POST",
          body: JSON.stringify(assessment),
        });
      }
    } finally {
      await refresh(detail.course.id);
    }
    notify("Example loaded. Make it your own.");
  }),
);
for (let i = 0; i <= 40; i++) {
  const angle = ((135 + i * 6.75) * Math.PI) / 180;
  const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
  line.setAttribute("x1", 160 + Math.cos(angle) * 109);
  line.setAttribute("y1", 140 + Math.sin(angle) * 109);
  line.setAttribute("x2", 160 + Math.cos(angle) * (i % 5 === 0 ? 101 : 105));
  line.setAttribute("y2", 140 + Math.sin(angle) * (i % 5 === 0 ? 101 : 105));
  $("#dial-ticks").append(line);
}
(async function boot() {
  try {
    state.session = await api("/session");
    state.scale = await api("/scale");
    const bands = $(".scale-bands");
    bands.replaceChildren();
    [...state.scale].reverse().forEach((band) => {
      const item = element("span");
      item.append(
        element("b", "", band.letter),
        element("small", "", band.minimum === 0 ? "<50" : `${band.minimum}+`),
      );
      bands.append(item);
    });
    bands.setAttribute(
      "aria-label",
      state.scale
        .map((band) => `${band.letter}: ${band.minimum} or above`)
        .join(", "),
    );
    $("#preview-note").hidden = state.session.storage !== "preview";
    await refresh();
  } catch (error) {
    showError($("#app-error"), error.message);
  }
})();
