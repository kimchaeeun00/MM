package com.moneymate.repository;

import com.moneymate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 찾기 위한 메서드 추가
    User findByUsername(String username);
}
