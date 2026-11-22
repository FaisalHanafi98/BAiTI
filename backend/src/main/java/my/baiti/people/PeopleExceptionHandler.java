package my.baiti.people;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import my.baiti.common.ApiError;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {UserAdminController.class, RoleController.class})
public class PeopleExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> invalidFields(
      HttpServletRequest request, MethodArgumentNotValidException failure) {
    return ApiError.response(
        request,
        400,
        "Invalid request",
        failure.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiError.InvalidField(error.getField(), error.getDefaultMessage()))
            .toList());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> invalidRequest(HttpServletRequest request) {
    return ApiError.response(request, 400, "Invalid request");
  }

  @ExceptionHandler(PeopleQueryException.class)
  public ResponseEntity<ApiError> invalidSort(HttpServletRequest request) {
    return ApiError.response(
        request,
        400,
        "Invalid request",
        List.of(new ApiError.InvalidField("sort", "Unsupported sort field")));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> conflict(
      HttpServletRequest request, DataIntegrityViolationException failure) {
    for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraint) {
        if ("uq_users_tenant_email".equals(constraint.getConstraintName())) {
          return ApiError.response(
              request,
              409,
              "An account with this email already exists",
              List.of(
                  new ApiError.InvalidField("email", "An account with this email already exists")));
        }
        if ("23503".equals(constraint.getSQLState())) {
          return ApiError.response(request, 409, "The selected role changed during the request");
        }
        break;
      }
    }
    throw failure;
  }
}
