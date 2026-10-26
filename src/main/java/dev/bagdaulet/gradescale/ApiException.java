package dev.bagdaulet.gradescale;

public class ApiException extends RuntimeException {
  private final int status;
  private final String code;

  public ApiException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static ApiException notFound() {
    return new ApiException(404, "NOT_FOUND", "This course or assessment was not found.");
  }

  public static ApiException invalid(String message) {
    return new ApiException(422, "VALIDATION_ERROR", message);
  }
}
