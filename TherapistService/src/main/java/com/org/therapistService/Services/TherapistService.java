package com.org.therapistService.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.therapistService.Assembler.TherapistAssembler;
import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Entity.TherapistServicesDto;
import com.org.therapistService.Repository.TherapistRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

@Service
public class TherapistService {

	@Autowired
	private TherapistRepository therapistRepository;
	
	@Autowired
	private TherapistServicesRepository therapistServicesRepository;
	
	private TherapistAssembler therapistAssembler = new TherapistAssembler();
	
	public List<TherapistDto> getAllTherapists(){
		List<Therapist> therapistList = therapistRepository.findAll();
		List<TherapistDto> therapistDtoList = new ArrayList<TherapistDto>();
		TherapistDto therapistDto;
		for(Therapist therapist : therapistList) {
			therapistDto = therapistAssembler.assembleEntityToDto(therapist);
			therapistDtoList.add(therapistDto);
		}
		return therapistDtoList;
	}
	
	public void createTherapist(TherapistDto therapistDto) {
		Therapist therapist = therapistAssembler.assembleDtoToEntity(therapistDto);
		therapistRepository.save(therapist);
	}
	
	public void createTherapistServices(TherapistServicesDto therapistServicesDto) {
		TherapistServices therapistServices = therapistAssembler.assembleDtoToEntity(therapistServicesDto);
		therapistServicesRepository.save(therapistServices);
	}
}
