package com.goormthonuniv.posts3.controller;

import com.goormthonuniv.posts3.entity.Post;
import com.goormthonuniv.posts3.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class UploadController {

    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("image") MultipartFile image) {

        try{
            String imageUrl = postService.uploadImageToS3(image);

            Post post = Post.builder()
                    .title(title)
                    .content(content)
                    .imageUrl(imageUrl)
                    .build();

            Post savedPost = postService.savePost(post);

            return ResponseEntity.ok(savedPost);
        } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("게시글 작성 중 오류가 발생했습니다.오류 : " + e.getMessage());
        }
    }
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        Optional<Post> post = postService.getPostById(id);

        return post.<ResponseEntity<?>>map(ResponseEntity::ok) // Optional<Post> → ResponseEntity<Post>
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("해당 게시글이 존재하지 않습니다."));
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            Optional<Post> optionalPost = postService.getPostById(id);
            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("수정할 게시글이 없습니다.");
            }

            Post existingPost = optionalPost.get();
            String imageUrl = existingPost.getImageUrl();

            if (image != null && !image.isEmpty()) {
                imageUrl = postService.uploadImageToS3(image);
            }

            existingPost.update(title, content, imageUrl);

            Post savedPost = postService.savePost(existingPost);
            return ResponseEntity.ok(savedPost);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게시글 수정 중 오류: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        boolean deleted = postService.deletePost(id);
        if (deleted) {
            return ResponseEntity.ok("게시글이 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("삭제할 게시글이 없습니다.");
        }
    }

    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<?> getPresignedUrl(@PathVariable Long id) {
        Optional<Post> optionalPost = postService.getPostById(id);
        if (optionalPost.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 게시글이 존재하지 않습니다.");
        }

        Post post = optionalPost.get();
        String imageUrl = post.getImageUrl();

        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("이미지가 없습니다.");
        }

        URL presignedUrl = postService.generatePresignedUrl(imageUrl);
        return ResponseEntity.ok(presignedUrl.toString());
    }

}
