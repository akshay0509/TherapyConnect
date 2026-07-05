package com.org.therapistService.Services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.therapistService.Dto.TherapistAvailabilityRulesDto;
import com.org.therapistService.Dto.TherapistServicesDto;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

/**
 * Verifies that every schedule-defining change (availability rules and
 * therapist services) triggers an immediate 7-day slot regeneration, and
 * that a regeneration failure propagates so the whole transaction rolls
 * back — the schedule definition and the slots can never disagree.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TherapistServiceScheduleChangeTest {

	@Mock
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Mock
	private TherapistServicesRepository therapistServicesRepository;

	@Mock
	private AvailabilitySlotService availabilitySlotService;

	@InjectMocks
	private TherapistService therapistService;

	private static final String THERAPIST_ID = "THR12345";

	private TherapistAvailabilityRules rule() {
		TherapistAvailabilityRules rule = new TherapistAvailabilityRules();
		rule.setRuleId("RUL1");
		rule.setTherapistId(THERAPIST_ID);
		rule.setDayOfWeek(1);
		rule.setStartTime(LocalTime.of(9, 0));
		rule.setEndTime(LocalTime.of(17, 0));
		rule.setActive(true);
		return rule;
	}

	private TherapistAvailabilityRulesDto ruleDto() {
		TherapistAvailabilityRulesDto dto = new TherapistAvailabilityRulesDto();
		dto.setTherapistId(THERAPIST_ID);
		dto.setDayOfWeek(1);
		dto.setStartTime(LocalTime.of(9, 0));
		dto.setEndTime(LocalTime.of(17, 0));
		dto.setIsActive(true);
		return dto;
	}

	private TherapistServices serviceEntity() {
		TherapistServices service = new TherapistServices();
		service.setServiceId("SRV1");
		service.setTherapistId(THERAPIST_ID);
		service.setDuration(60);
		service.setPrice(new BigDecimal("1500"));
		service.setActive(true);
		return service;
	}

	private TherapistServicesDto serviceDto() {
		TherapistServicesDto dto = new TherapistServicesDto();
		dto.setTherapistId(THERAPIST_ID);
		dto.setDuration(60);
		dto.setPrice(new BigDecimal("1500"));
		dto.setIsActive(true);
		return dto;
	}

	private void verifyRegenerationTriggered() throws JsonProcessingException {
		verify(availabilitySlotService).generateAvailabilitySlots(
				eq(THERAPIST_ID), any(LocalDate.class), any(LocalDate.class));
	}

	// ── availability rules ───────────────────────────────────────────────────

	@Test
	void creatingRulesTriggersImmediateRegeneration() throws Exception {
		when(therapistAvailabilityRulesRepository.saveAll(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		therapistService.createTherapistAvailabilityRules(List.of(ruleDto()));

		verifyRegenerationTriggered();
	}

	@Test
	void creatingEmptyRuleListDoesNotRegenerate() {
		therapistService.createTherapistAvailabilityRules(List.of());

		verifyNoInteractions(availabilitySlotService);
	}

	@Test
	void updatingRuleTriggersImmediateRegeneration() throws Exception {
		when(therapistAvailabilityRulesRepository.findByRuleIdAndTherapistId("RUL1", THERAPIST_ID))
				.thenReturn(Optional.of(rule()));
		when(therapistAvailabilityRulesRepository.save(any())).thenAnswer(i -> i.getArgument(0));

		therapistService.updateAvailabilityRule(THERAPIST_ID, "RUL1", ruleDto());

		verifyRegenerationTriggered();
	}

	@Test
	void deletingRuleTriggersImmediateRegeneration() throws Exception {
		when(therapistAvailabilityRulesRepository.findByRuleIdAndTherapistId("RUL1", THERAPIST_ID))
				.thenReturn(Optional.of(rule()));

		therapistService.deleteAvailabilityRule(THERAPIST_ID, "RUL1");

		verify(therapistAvailabilityRulesRepository).delete(any(TherapistAvailabilityRules.class));
		verifyRegenerationTriggered();
	}

	// ── therapist services (slots carry service duration + price) ────────────

	@Test
	void creatingServiceTriggersImmediateRegeneration() throws Exception {
		therapistService.createTherapistServices(serviceDto());

		verifyRegenerationTriggered();
	}

	@Test
	void updatingServiceTriggersImmediateRegeneration() throws Exception {
		when(therapistServicesRepository.findByServiceIdAndTherapistId("SRV1", THERAPIST_ID))
				.thenReturn(Optional.of(serviceEntity()));
		when(therapistServicesRepository.save(any())).thenAnswer(i -> i.getArgument(0));

		therapistService.updateTherapistService(THERAPIST_ID, "SRV1", serviceDto());

		verifyRegenerationTriggered();
	}

	@Test
	void deletingServiceTriggersImmediateRegeneration() throws Exception {
		when(therapistServicesRepository.findByServiceIdAndTherapistId("SRV1", THERAPIST_ID))
				.thenReturn(Optional.of(serviceEntity()));

		therapistService.deleteTherapistService(THERAPIST_ID, "SRV1");

		verify(therapistServicesRepository).delete(any(TherapistServices.class));
		verifyRegenerationTriggered();
	}

	// ── failure semantics ─────────────────────────────────────────────────────

	@Test
	void regenerationFailurePropagatesSoTheRuleChangeRollsBack() throws Exception {
		when(therapistAvailabilityRulesRepository.findByRuleIdAndTherapistId("RUL1", THERAPIST_ID))
				.thenReturn(Optional.of(rule()));
		doThrow(new JsonProcessingException("serialization failed") {})
				.when(availabilitySlotService)
				.generateAvailabilitySlots(eq(THERAPIST_ID), any(LocalDate.class), any(LocalDate.class));

		// unchecked wrapper propagates → @Transactional rolls the rule delete back
		assertThrows(IllegalStateException.class,
				() -> therapistService.deleteAvailabilityRule(THERAPIST_ID, "RUL1"));
	}
}
