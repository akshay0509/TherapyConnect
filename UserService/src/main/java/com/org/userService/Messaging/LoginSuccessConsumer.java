package com.org.userService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.org.events.login.LoginSuccessEvent;
import com.org.userService.Entity.User;
import com.org.userService.Entity.UserLoginAudit;
import com.org.userService.Repository.UserLoginAuditRepository;
import com.org.userService.Repository.UserRepository;

import jakarta.transaction.Transactional;

@Component
public class LoginSuccessConsumer {

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserLoginAuditRepository userLoginAuditRepository;
	
	private static final String topic = "auth-login-success";
	
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(LoginSuccessEvent loginSuccessEvent) {
		User user = userRepository.findByUsernameAndIsEnabledTrueAndIsAccountLockedFalse(loginSuccessEvent.getUsername());
		
		user.setLastLoginTime(loginSuccessEvent.getTimestamp());
		user.setLastLoginIp(loginSuccessEvent.getIpAddress());
		user.setFailedAttempts(0);
		
		userLoginAuditRepository.save(
				UserLoginAudit.success(
						loginSuccessEvent.getUserId(),
						loginSuccessEvent.getUsername(),
						loginSuccessEvent.getIpAddress(),
						loginSuccessEvent.getUserAgent()
		        )
		);
	}
}
