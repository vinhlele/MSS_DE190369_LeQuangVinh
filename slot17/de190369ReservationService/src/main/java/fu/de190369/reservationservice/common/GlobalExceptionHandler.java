package fu.de190369.reservationservice.common;

import fu.de190369.reservationservice.dto.ApiResponseDTO;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiResponseDTO.error(2, errorMessage));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponseDTO> handleMethodValidationExceptions(HandlerMethodValidationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiResponseDTO.error(2, "Validation failure: " + ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiResponseDTO.error(2, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponseDTO> handleTypeMismatch(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiResponseDTO.error(2, "Invalid input data: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleAllExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(0, "Internal server error: " + ex.getMessage()));
    }
}
