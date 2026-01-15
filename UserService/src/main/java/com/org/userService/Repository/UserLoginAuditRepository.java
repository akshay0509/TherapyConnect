package com.org.userService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.userService.Entity.UserLoginAudit;

@Repository
public interface UserLoginAuditRepository extends JpaRepository<UserLoginAudit, Long>{

}
