package com.org.therapistService.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.therapistService.Assembler.TherapistAssembler;
import com.org.therapistService.Entity.Therapist;
import com.org.therapistService.Entity.TherapistAppointments;
import com.org.therapistService.Entity.TherapistAppointmentsDto;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityDto;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityOverridesDto;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistAvailabilityRulesDto;
import com.org.therapistService.Entity.TherapistDto;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Entity.TherapistServicesDto;
import com.org.therapistService.Repository.TherapistAppointmentsRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

@Service
public class TherapistService {

	@Autowired
	private TherapistRepository therapistRepository;
	
	@Autowired
	private TherapistServicesRepository therapistServicesRepository;
	
	@Autowired
	private TherapistAppointmentsRepository therapistAppointmentsRepository;
	
	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;
	
	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;
	
	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;
	
	private TherapistAssembler therapistAssembler = new TherapistAssembler();
	
	public void createTherapist(TherapistDto therapistDto) {
		Therapist therapist = therapistAssembler.assembleDtoToEntity(therapistDto);
		therapistRepository.save(therapist);
	}
	
	public List<TherapistDto> getAllTherapists(){
		List<Therapist> list = therapistRepository.findAll();
		List<TherapistDto> dtoList = new ArrayList<TherapistDto>();
		TherapistDto dto;
		for(Therapist rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public void createTherapistServices(TherapistServicesDto therapistServicesDto) {
		TherapistServices therapistServices = therapistAssembler.assembleDtoToEntity(therapistServicesDto);
		therapistServicesRepository.save(therapistServices);
	}
	
	public List<TherapistServicesDto> getAllTherapistServices(){
		List<TherapistServices> list = therapistServicesRepository.findAll();
		List<TherapistServicesDto> dtoList = new ArrayList<TherapistServicesDto>();
		TherapistServicesDto dto;
		for(TherapistServices rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public List<TherapistServicesDto> getTherapistServices(String therapistId){
		List<TherapistServices> list = therapistServicesRepository.findByTherapistId(therapistId);
		List<TherapistServicesDto> dtoList = new ArrayList<TherapistServicesDto>();
		TherapistServicesDto dto;
		for(TherapistServices rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public List<TherapistAvailabilityDto> getTherapistAvailability(String therapistId) {
		List<TherapistAvailability> list = therapistAvailabilityRepository.findByTherapistId(therapistId);
		List<TherapistAvailabilityDto> dtoList = new ArrayList<TherapistAvailabilityDto>();
		TherapistAvailabilityDto dto;
		for(TherapistAvailability rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public List<TherapistAvailabilityDto> getAllTherapistAvailability(){
		List<TherapistAvailability> list = therapistAvailabilityRepository.findAll();
		List<TherapistAvailabilityDto> dtoList = new ArrayList<TherapistAvailabilityDto>();
		TherapistAvailabilityDto dto;
		for(TherapistAvailability rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public void createTherapistAvailabilityRules(TherapistAvailabilityRulesDto therapistAvailabilityRulesDto) {
		TherapistAvailabilityRules therapistAvailabilityRules = therapistAssembler.assembleDtoToEntity(therapistAvailabilityRulesDto);
		therapistAvailabilityRulesRepository.save(therapistAvailabilityRules);
	}
	
	public List<TherapistAvailabilityRulesDto> getAllTherapistAvailabilityRules(){
		List<TherapistAvailabilityRules> list = therapistAvailabilityRulesRepository.findAll();
		List<TherapistAvailabilityRulesDto> dtoList = new ArrayList<TherapistAvailabilityRulesDto>();
		TherapistAvailabilityRulesDto dto;
		for(TherapistAvailabilityRules rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public void createTherapistAvailabilityOverrides(TherapistAvailabilityOverridesDto therapistAvailabilityOverridesDto) {
		TherapistAvailabilityOverrides therapistAvailabilityOverrides = therapistAssembler.assembleDtoToEntity(therapistAvailabilityOverridesDto);
		therapistAvailabilityOverridesRepository.save(therapistAvailabilityOverrides);
	}
	
	public List<TherapistAvailabilityOverridesDto> getAllTherapistAvailabilityOverrides(){
		List<TherapistAvailabilityOverrides> list = therapistAvailabilityOverridesRepository.findAll();
		List<TherapistAvailabilityOverridesDto> dtoList = new ArrayList<TherapistAvailabilityOverridesDto>();
		TherapistAvailabilityOverridesDto dto;
		for(TherapistAvailabilityOverrides rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
	public void createTherapistAppointments(TherapistAppointmentsDto therapistAppointmentsDto) {
		TherapistAppointments therapistAppointments = therapistAssembler.assembleDtoToEntity(therapistAppointmentsDto);
		therapistAppointmentsRepository.save(therapistAppointments);
	}
	
	public List<TherapistAppointmentsDto> getAllTherapistAppointments(){
		List<TherapistAppointments> list = therapistAppointmentsRepository.findAll();
		List<TherapistAppointmentsDto> dtoList = new ArrayList<TherapistAppointmentsDto>();
		TherapistAppointmentsDto dto;
		for(TherapistAppointments rec : list) {
			dto = therapistAssembler.assembleEntityToDto(rec);
			dtoList.add(dto);
		}
		return dtoList;
	}
	
}
