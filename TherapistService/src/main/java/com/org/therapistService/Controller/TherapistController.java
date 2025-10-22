package com.org.therapistService.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Services.TherapistService;

@RestController
public class TherapistController {

	@Autowired
	private TherapistService clientService;
	
	@GetMapping("/therapists")
	public List<TherapistDto> getAllTherapists(){
		return clientService.getAllTherapists();
	}
	
	@PostMapping("/therapist")
	public void createTherapist(@RequestBody TherapistDto therapistDto) {
		clientService.createTherapist(therapistDto);
	}
}
