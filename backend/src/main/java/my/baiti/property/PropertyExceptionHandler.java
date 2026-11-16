package my.baiti.property;

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

@RestControllerAdvice(assignableTypes = {BlockController.class, UnitController.class})
public class PropertyExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> invalidFields(
      HttpServletRequest request, MethodArgumentNotValidException failure) {
    var fields =
        failure.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiError.InvalidField(error.getField(), error.getDefaultMessage()))
            .toList();
    return ApiError.response(request, 400, "Invalid request", fields);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> invalidBody(HttpServletRequest request) {
    return ApiError.response(request, 400, "Invalid request");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> invalidParameter(
      HttpServletRequest request, MethodArgumentTypeMismatchException failure) {
    return invalidQuery(request, new PropertyQueryException(failure.getName(), "Invalid value"));
  }

  @ExceptionHandler(PropertyQueryException.class)
  public ResponseEntity<ApiError> invalidQuery(
      HttpServletRequest request, PropertyQueryException failure) {
    return ApiError.response(
        request,
        400,
        "Invalid request",
        List.of(new ApiError.InvalidField(failure.field(), failure.getMessage())));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> constraintConflict(
      HttpServletRequest request, DataIntegrityViolationException failure) {
    for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraint) {
        if ("uq_blocks_tenant_name".equals(constraint.getConstraintName())) {
          return duplicate(request, "name", "A block with this name already exists");
        }
        if ("uq_units_tenant_block_unit".equals(constraint.getConstraintName())) {
          return duplicate(request, "unitNo", "This block already has a unit with this number");
        }
        if ("23503".equals(constraint.getSQLState()) && "DELETE".equals(request.getMethod())) {
          return ApiError.response(request, 409, "Other records still reference this resource");
        }
        if ("23503".equals(constraint.getSQLState())
            && "units_block_id_fkey".equals(constraint.getConstraintName())) {
          return ApiError.response(request, 409, "The selected block changed during the request");
        }
        break;
      }
    }
    throw failure;
  }

  private static ResponseEntity<ApiError> duplicate(
      HttpServletRequest request, String field, String message) {
    return ApiError.response(
        request,
        409,
        "A property record already has this value",
        List.of(new ApiError.InvalidField(field, message)));
  }
}
