package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.AppointmentProjection;

@Repository
public interface AppointmentProjectionRepository extends JpaRepository<AppointmentProjection, String>{

	AppointmentProjection findByAppointmentIdAndTherapistId(String appointmentId, String therapistId);
}
