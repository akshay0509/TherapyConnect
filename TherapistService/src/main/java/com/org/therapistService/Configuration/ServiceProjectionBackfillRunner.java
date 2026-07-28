package com.org.therapistService.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.org.therapistService.Services.TherapistService;

/**
 * One-shot deployment aid. Enable for one TherapistService start with:
 * app.service-projection-backfill.enabled=true
 */
@Component
@ConditionalOnProperty(
		name = "app.service-projection-backfill.enabled",
		havingValue = "true")
public class ServiceProjectionBackfillRunner implements ApplicationRunner {

	private final TherapistService therapistService;

	public ServiceProjectionBackfillRunner(TherapistService therapistService) {
		this.therapistService = therapistService;
	}

	@Override
	public void run(ApplicationArguments args) {
		therapistService.publishAllServiceDefinitions();
	}
}
