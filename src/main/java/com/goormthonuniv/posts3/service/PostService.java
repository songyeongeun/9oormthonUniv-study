package com.goormthonuniv.posts3.service;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.goormthonuniv.posts3.entity.Post;
import com.goormthonuniv.posts3.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private PostRepository postRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadImageToS3(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }

        String fileName = UUID.randomUUID().toString() + "-" + image.getOriginalFilename();
        File file = null;

        try {
            file = convertMultipartFileToFile(image);
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file));
            return amazonS3.getUrl(bucketName, fileName).toString();
        } catch (Exception e) {
            if (amazonS3.doesObjectExist(bucketName, fileName)) {
                amazonS3.deleteObject(bucketName, fileName);
            }
            throw new RuntimeException("이미지 업로드 실패", e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }


    private File convertMultipartFileToFile(MultipartFile image) throws IOException {
        File file = File.createTempFile(UUID.randomUUID().toString(), "-" + image.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(image.getBytes());
        }
        return file;
    }

    public Post savePost(Post post) {
        return postRepository.save(post);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public Post updatePost(Long id, Post newPost) {
        return postRepository.findById(id).map(existing -> {
            existing.update(newPost.getTitle(), newPost.getContent(), newPost.getImageUrl());
            return postRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));
    }

    public boolean deletePost(Long id) {
        return postRepository.findById(id).map(post -> {

            String imageUrl = post.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    String fileName = extractFileNameFromUrl(imageUrl);
                    amazonS3.deleteObject(bucketName, fileName);
                } catch (Exception e) {
                    System.err.println("[S3 삭제 실패] " + imageUrl + " → " + e.getMessage());
                }
            }

            postRepository.deleteById(id);
            return true;

        }).orElse(false);
    }



    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    public URL generatePresignedUrl(String imageUrl) {
        String fileName = extractFileNameFromUrl(imageUrl);

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expiration.setTime(expTimeMillis + 1000 * 60 * 60); // 1시간

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, fileName)
                        .withMethod(com.amazonaws.HttpMethod.GET)
                        .withExpiration(expiration);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
    }

}
