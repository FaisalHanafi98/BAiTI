package my.baiti.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

public record ApiError(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    List<InvalidField> errors) {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  public ApiError {
    errors = List.copyOf(errors);
  }

  public static ResponseEntity<ApiError> response(
      HttpServletRequest request, int status, String detail) {
    return response(request, status, detail, List.of());
  }

  public static ResponseEntity<ApiError> response(
      HttpServletRequest request, int status, String detail, List<InvalidField> errors) {
    return ResponseEntity.status(status)
        .headers(headers(status))
        .body(
            new ApiError(
                "about:blank",
                HttpStatus.valueOf(status).getReasonPhrase(),
                status,
                detail,
                request.getRequestURI(),
                errors));
  }

  public static void write(
      HttpServletRequest request, HttpServletResponse response, int status, String detail)
      throws IOException {
    write(request, response, status, detail, List.of());
  }

  public static void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String detail,
      List<InvalidField> errors)
      throws IOException {
    var result = response(request, status, detail, errors);
    response.setStatus(status);
    response.setCharacterEncoding("UTF-8");
    result
        .getHeaders()
        .forEach((name, values) -> values.forEach(value -> response.setHeader(name, value)));
    response.getWriter().write(JSON.writeValueAsString(result.getBody()));
  }

  private static HttpHeaders headers(int status) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    headers.setCacheControl("no-store");
    if (status == 401) {
      headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    if (status == 429) {
      headers.set(HttpHeaders.RETRY_AFTER, "60");
    }
    return headers;
  }

  public record InvalidField(String field, String message) {}
}
