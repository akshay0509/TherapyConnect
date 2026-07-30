package com.org.therapistService.Dto;

import lombok.Data;

/**
 * Creates a service together with its first delivery mode, in one transaction.
 *
 * A service with no mode is silently unbookable: AppointmentService derives
 * duration and fee from the selected mode, so booking fails later with a
 * misleading "service not available". Creating the two in separate calls leaves
 * a window where the service commits and the mode does not — producing exactly
 * the broken state this is meant to prevent — so they are submitted together.
 */
@Data
public class ServiceWithModeRequest {

    private TherapistServicesDto service;
    private TherapyDeliveryModeDto mode;
}
