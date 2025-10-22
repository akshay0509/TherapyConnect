package com.org.therapistService.Assembler;

import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistDto;

public class TherapistAssembler {

	public Therapist assembleDtoToEntity(TherapistDto therapistDto) {
		Therapist therapist = new Therapist();
		therapist.setFirstName(therapistDto.getFirstName());
		therapist.setLastName(therapistDto.getLastName());
		therapist.setDob(therapistDto.getDob());
		therapist.setEmail(therapistDto.getEmail());
		therapist.setGender(therapistDto.getGender());
		therapist.setPhoneNumber(therapistDto.getPhoneNumber());
		therapist.setYearsOfExperience(therapistDto.getYearsOfExperience());
		
		return therapist;
	}
	
	public TherapistDto assembleEntityToDto(Therapist therapist) {
		TherapistDto therapistDto = new TherapistDto();
		therapistDto.setFirstName(therapist.getFirstName());
		therapistDto.setLastName(therapist.getLastName());
		therapistDto.setDob(therapist.getDob());
		therapistDto.setEmail(therapist.getEmail());
		therapistDto.setGender(therapist.getGender());
		therapistDto.setPhoneNumber(therapist.getPhoneNumber());
		therapistDto.setYearsOfExperience(therapist.getYearsOfExperience());
		
		return therapistDto;
	}
	
}
