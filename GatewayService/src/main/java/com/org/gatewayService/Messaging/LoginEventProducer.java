package com.org.gatewayService.Messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.org.events.login.LoginFailureEvent;
import com.org.events.login.LoginSuccessEvent;

@Service
public class LoginEventProducer {

	private static final Logger logger = LoggerFactory.getLogger(LoginEventProducer.class);

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private static final String LOGIN_SUCCESS_TOPIC = "auth-login-success";
	private static final String LOGIN_FAILURE_TOPIC = "auth-login-failure";

	public void publishLoginSuccess(LoginSuccessEvent event) {
		logger.debug("Publishing login success event for userId={}", event.getUserId());
		kafkaTemplate.send(LOGIN_SUCCESS_TOPIC, event.getUserId().toString(), event);
	}

	public void publishLoginFailure(LoginFailureEvent event) {
		logger.debug("Publishing login failure event for username={}", event.getUsername());
		kafkaTemplate.send(LOGIN_FAILURE_TOPIC, event.getUsername(), event);
	}
}
