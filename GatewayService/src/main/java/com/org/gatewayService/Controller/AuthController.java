package com.org.gatewayService.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.gatewayService.Dto.LoginRequestDto;
import com.org.gatewayService.Utility.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@PostMapping("/login")
	public Map<String, String> login(@RequestBody LoginRequestDto loginRequest){
		
		String token = JwtUtil.generateToken(loginRequest.getUsername(),
											List.of("read", "write"),
											List.of("THERAPIST", "ADMIN")); 
		return Map.of("token", token);
	}
}
