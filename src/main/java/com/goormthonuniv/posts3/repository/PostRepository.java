package com.goormthonuniv.posts3.repository;

import com.goormthonuniv.posts3.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
