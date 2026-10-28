package fu.de190369.userservice.controller;

import fu.de190369.userservice.dto.ApiResponse;
import fu.de190369.userservice.dto.UserDTO;
import fu.de190369.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class DE190369UserController {

    private final UserService userService;

    public DE190369UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody UserDTO dto) {
        ApiResponse res = userService.createUser(dto);
        if (res.getStatus() == 3) {
            return ResponseEntity.status(HttpStatus.IM_USED).body(res); // 226
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
        }
        if (res.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(res); // 201
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res); // 500
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Long userId, @RequestBody UserDTO dto) {
        // Validate partial update fields if provided
        if (dto.getEmail() != null && !dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "email is invalid"));
        }
        if (dto.getMaxDailyPosts() != null && (dto.getMaxDailyPosts() < 1 || dto.getMaxDailyPosts() > 10)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "maxDailyPosts must be between 1 and 10"));
        }
        if (dto.getRatePerPost() != null && dto.getRatePerPost().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "ratePerPost must be > 0"));
        }
        if (dto.getLevel() != null && dto.getLevel() < 1) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "level must be >= 1"));
        }

        ApiResponse res = userService.updateUser(userId, dto);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        if (res.getStatus() == 3) {
            return ResponseEntity.status(HttpStatus.IM_USED).body(res); // 226
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
        }
        if (res.getStatus() == 1) {
            return ResponseEntity.ok(res); // 200
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        ApiResponse res = userService.getUserById(userId);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        return ResponseEntity.ok(res); // 200
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deactivateUser(@PathVariable Long userId) {
        ApiResponse res = userService.deactivateUser(userId);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        return ResponseEntity.ok(res); // 200
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        // Validate size max 100
        if (size > 100 || size < 1) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "size must be between 1 and 100"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "page must be >= 0"));
        }

        Pageable pageable = PageRequest.of(page, size);
        ApiResponse res = userService.getUserList(role, status, pageable);
        return ResponseEntity.ok(res);
    }
}
