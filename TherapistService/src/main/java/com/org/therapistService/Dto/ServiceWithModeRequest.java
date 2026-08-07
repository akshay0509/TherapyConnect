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

    /**
     * Single-mode form, kept so an older client keeps working. Prefer modes.
     */
    private TherapyDeliveryModeDto mode;

    /**
     * All modes the service launches with. A therapist who works both online and
     * from a clinic offers one service in two ways, and making them create the
     * service and then come back to add the second mode is an artificial detour
     * — worse, it leaves the service half-configured if they never return.
     * Created in the same transaction as the service.
     */
    private java.util.List<TherapyDeliveryModeDto> modes;
}
