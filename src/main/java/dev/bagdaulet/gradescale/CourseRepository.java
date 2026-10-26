package dev.bagdaulet.gradescale;

import static dev.bagdaulet.gradescale.Models.*;

import java.util.UUID;

public interface CourseRepository {
  CoursePage list(String owner, int page);

  CourseDetail create(String owner, CourseInput input);

  CourseDetail detail(String owner, UUID id);

  CourseDetail update(String owner, UUID id, CourseInput input);

  void delete(String owner, UUID id);

  Assessment add(String owner, UUID courseId, AssessmentInput input);

  Assessment patch(String owner, UUID assessmentId, AssessmentPatch input);

  void remove(String owner, UUID assessmentId);

  void checkHealth();

  String storage();
}
