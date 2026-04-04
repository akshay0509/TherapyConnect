package com.org.appointmentService.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentScheduleViewDto {

    private List<AvailabilityResponseDto> slots;
    private List<AppointmentScheduleAppointmentDto> appointments;
    private List<AppointmentScheduleOverrideDto> overrides;
}
