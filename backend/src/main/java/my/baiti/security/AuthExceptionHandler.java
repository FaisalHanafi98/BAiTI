package my.baiti.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import my.baiti.common.ApiError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

  @ExceptionHandler(LoginRateLimitException.class)
  public void rateLimited(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ApiError.write(request, response, 429, "Too many requests");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public void requestDenied(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ApiError.write(request, response, 403, "Access denied");
  }

  @ExceptionHandler(BadCredentialsException.class)
  public void authenticationFailed(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ApiError.write(request, response, 401, "Authentication failed");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public void invalidFields(
      HttpServletRequest request,
      HttpServletResponse response,
      MethodArgumentNotValidException failure)
      throws IOException {
    var errors =
        failure.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new ApiError.InvalidField(
                        error.getField(),
                        error.getDefaultMessage() == null
                            ? "Invalid value"
                            : error.getDefaultMessage()))
            .toList();
    ApiError.write(request, response, 400, "Invalid request", errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public void invalidBody(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ApiError.write(request, response, 400, "Invalid request", List.of());
  }
}
