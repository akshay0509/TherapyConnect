package com.org.therapistService.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.therapistService.Services.TherapistService;

@RestController
@RequestMapping("/internal/therapist")
public class InternalTherapistController {

	@Autowired
	TherapistService therapistService;
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<String> getTherapistIdByUserId(@PathVariable String userId){
		String therapistId = therapistService.getTherapistIdByUserId(userId);
		return ResponseEntity.ok(therapistId);
	}
}
