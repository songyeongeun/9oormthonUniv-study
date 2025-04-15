package com.goormthonuniv.posts3.controller;

import com.goormthonuniv.posts3.entity.Post;
import com.goormthonuniv.posts3.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
