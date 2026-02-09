package com.org.gatewayService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.org.events.login.LoginFailureEvent;
import com.org.events.login.LoginSuccessEvent;

@Service
public class LoginEventProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;
	
	/*
	@Autowired
	private ObjectMapper objectMapper;
	*/
	
	private static final String LOGIN_SUCCESS_TOPIC = "auth-login-success";
	private static final String LOGIN_FAILURE_TOPIC = "auth-login-failure";

	public void publishLoginSuccess(LoginSuccessEvent event) {
		System.out.println("publishLoginSuccess..");
		/*
		String jsonMessage;
		try {
			jsonMessage =objectMapper.writeValueAsString(event);
			System.out.println("success..");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			jsonMessage = null;
		}
        kafkaTemplate.send(LOGIN_SUCCESS_TOPIC, event.getUserId().toString(), jsonMessage);
		 */
		kafkaTemplate.send(LOGIN_SUCCESS_TOPIC, event.getUserId().toString(), event);
		System.out.println("exiting..");
	}

	public void publishLoginFailure(LoginFailureEvent event) {
		System.out.println("publishLoginFailure..");
		/*
    	String jsonMessage;
		try {
			jsonMessage = objectMapper.writeValueAsString(event);
			System.out.println("success..");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			jsonMessage = null;
		}
        kafkaTemplate.send(LOGIN_FAILURE_TOPIC, event.getUsername(), jsonMessage);
		 */
		kafkaTemplate.send(LOGIN_FAILURE_TOPIC, event.getUsername(), event);
		System.out.println("exiting..");
	}
}
