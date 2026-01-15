package com.org.gatewayService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.gatewayService.Dto.LoginFailureEvent;
import com.org.gatewayService.Dto.LoginSuccessEvent;

@Service
public class LoginEventProducer {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	private static final String LOGIN_SUCCESS_TOPIC = "auth-login-success";
    private static final String LOGIN_FAILURE_TOPIC = "auth-login-failure";
	
	public void publishLoginSuccess(LoginSuccessEvent event) {
		String jsonMessage;
		try {
			jsonMessage = new ObjectMapper().writeValueAsString(event);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			jsonMessage = null;
		}
        kafkaTemplate.send(LOGIN_SUCCESS_TOPIC, event.getUserId().toString(), jsonMessage);
    }

    public void publishLoginFailure(LoginFailureEvent event) {
    	String jsonMessage;
		try {
			jsonMessage = new ObjectMapper().writeValueAsString(event);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			jsonMessage = null;
		}
        kafkaTemplate.send(LOGIN_FAILURE_TOPIC, event.getUsername(), jsonMessage);
    }
}
