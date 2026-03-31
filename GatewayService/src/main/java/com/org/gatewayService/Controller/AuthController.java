package com.org.gatewayService.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.events.login.LoginFailureEvent;
import com.org.events.login.LoginSuccessEvent;
import com.org.gatewayService.Dto.AuthRequest;
import com.org.gatewayService.Dto.AuthResponse;
import com.org.gatewayService.Messaging.LoginEventProducer;
import com.org.gatewayService.Proxy.TherapistServiceProxy;
import com.org.gatewayService.Proxy.UserServiceProxy;
import com.org.gatewayService.Utility.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	@Autowired
	private UserServiceProxy userServiceProxy;
	
	@Autowired
	private TherapistServiceProxy therapistServiceProxy;
	
	@Autowired
    private JwtUtil jwtUtil;
	
	@Autowired
	LoginEventProducer loginEventProducer;

	@PostMapping("/login")
	public Map<String, String> login(@RequestBody AuthRequest authRequest, HttpServletRequest httpRequest){
		System.out.println("Calling user service and validating user info..");
		System.out.println("authRequest= "+authRequest);
		AuthResponse authResponse = userServiceProxy.validateUser(authRequest);
		System.out.println("authResponse= "+authResponse);
		if(!authResponse.isAuthenticated()) {
			
			LoginFailureEvent loginFailureEvent = new LoginFailureEvent();
			loginFailureEvent.setUserId(authResponse.getUserId());
			loginFailureEvent.setUsername(authRequest.getUsername());
			loginFailureEvent.setIpAddress(httpRequest.getRemoteAddr());
			loginFailureEvent.setUserAgent(httpRequest.getHeader("User-Agent"));
			loginFailureEvent.setTimestamp(Instant.now());
			loginFailureEvent.setReason(authResponse.getFailureReason().name());
	        
			loginEventProducer.publishLoginFailure(loginFailureEvent);
			return Map.of("Failure Reason", authResponse.getFailureReason().name());
		}
		
		LoginSuccessEvent loginSuccessEvent = new LoginSuccessEvent();
		loginSuccessEvent.setUserId(authResponse.getUserId());
		loginSuccessEvent.setUsername(authRequest.getUsername());
		loginSuccessEvent.setIpAddress(httpRequest.getRemoteAddr());
		loginSuccessEvent.setUserAgent(httpRequest.getHeader("User-Agent"));
		loginSuccessEvent.setTimestamp(Instant.now());

        loginEventProducer.publishLoginSuccess(loginSuccessEvent);
		
        String therapistId = null;
        
        try {
        	therapistId = therapistServiceProxy.getTherapistId(authResponse.getUserId());
        }
        catch (Exception e) {
        	System.out.println("No therapist profile found yet for userId=" + authResponse.getUserId());
        }
        
		String token = jwtUtil.generateToken(authRequest.getUsername(),
											List.of("read", "write"),
											authResponse.getRoles(),
											authResponse.getUserId(),
											therapistId); 
		return Map.of("token", token);
	}
}
