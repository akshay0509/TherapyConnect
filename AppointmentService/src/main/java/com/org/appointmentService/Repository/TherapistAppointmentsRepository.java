package com.org.appointmentService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapistAppointments;

@Repository
public interface TherapistAppointmentsRepository extends JpaRepository<TherapistAppointments, String>{

}
