package com.org.userService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.userService.Dto.LoginFailureEvent;
import com.org.userService.Entity.User;
import com.org.userService.Entity.UserLoginAudit;
import com.org.userService.Repository.UserLoginAuditRepository;
import com.org.userService.Repository.UserRepository;

import jakarta.transaction.Transactional;

@Component
public class LoginFailureConsumer {

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserLoginAuditRepository userLoginAuditRepository;
	
	private static final String topic = "auth-login-failure";
	private static final int MAX_FAILED_ATTEMPTS = 5;
	
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(String message) {
		try {
			LoginFailureEvent loginFailureEvent = new ObjectMapper().readValue(message, LoginFailureEvent.class);
			
	        userLoginAuditRepository.save(
					UserLoginAudit.failure(
							loginFailureEvent.getUsername(),
	                        loginFailureEvent.getIpAddress(),
	                        loginFailureEvent.getUserAgent(),
	                        loginFailureEvent.getReason()
	                )
	        );
	        
	        User user = userRepository.findByUsernameAndIsEnabledTrueAndIsAccountLockedFalse(loginFailureEvent.getUsername());
	        int failedAttempts = user.getFailedAttempts() + 1;
	        user.setFailedAttempts(failedAttempts);
	        
	        if(failedAttempts >= MAX_FAILED_ATTEMPTS) {
	        	user.setAccountLocked(true);
	        }
	        
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
