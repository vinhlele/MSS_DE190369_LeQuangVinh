package fu.de190369.postservice.controller;

import fu.de190369.postservice.dto.ApiResponse;
import fu.de190369.postservice.dto.PostDTO;
import fu.de190369.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class DE190369PostController {

    private final PostService postService;

    public DE190369PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createPost(@Valid @RequestBody PostDTO dto) {
        ApiResponse res = postService.createPost(dto);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        if (res.getStatus() == 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res); // 400
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
        }
        if (res.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(res); // 201
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse> updatePost(@PathVariable Long postId, @RequestBody PostDTO dto) {
        ApiResponse res = postService.updatePost(postId, dto);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
        }
        if (res.getStatus() == 1) {
            return ResponseEntity.ok(res); // 200
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse> getPostById(@PathVariable Long postId) {
        ApiResponse res = postService.getPostById(postId);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        return ResponseEntity.ok(res); // 200
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse> cancelPost(@PathVariable Long postId) {
        ApiResponse res = postService.cancelPost(postId);
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        return ResponseEntity.ok(res); // 200
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String advertiserName,
            @RequestParam(required = false) String status) {

        if (size > 100 || size < 1) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "size must be between 1 and 100"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "page must be >= 0"));
        }

        Pageable pageable = PageRequest.of(page, size);
        ApiResponse res = postService.getPostList(advertiserName, status, pageable);
        return ResponseEntity.ok(res);
    }
}
