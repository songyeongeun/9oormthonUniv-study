package com.goormthonuniv.posts3.service;

import com.goormthonuniv.posts3.entity.Post;
import com.goormthonuniv.posts3.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class PostService {

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private PostRepository postRepository;

    private String bucketName = "youngeun-bucket-01";

    public String uploadImageToS3(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
            }

            String fileName = UUID.randomUUID().toString() + "-" + image.getOriginalFilename();
            File file = convertMultipartFileToFile(image);
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file));

            file.delete();

            return amazonS3.getUrl(bucketName, fileName).toString();
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }

    private File convertMultipartFileToFile(MultipartFile image) throws IOException {
        File file = new File(image.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(image.getBytes());
        }
        return file;
    }

    public Post savePost(Post post) {
        return postRepository.save(post);
    }
}
