package com.org.userService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.userService.Entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>{

	User findByUsernameAndIsEnabledTrueAndIsAccountLockedFalse(String username);
	User findByUsername(String username);
}
