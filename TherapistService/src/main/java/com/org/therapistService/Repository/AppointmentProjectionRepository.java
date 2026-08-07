package com.org.therapistService.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.therapistService.Dto.ClientEnrichmentDto;
import com.org.therapistService.Dto.EarningsSessionDto;
import com.org.therapistService.Entity.AppointmentProjection;

@Repository
public interface AppointmentProjectionRepository extends JpaRepository<AppointmentProjection, String>{

	AppointmentProjection findByAppointmentIdAndTherapistId(
			String appointmentId,
			String therapistId
			);

	long countByTherapistIdAndStatusInAndStartTimeBetween(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime start,
			LocalDateTime end
			);

	long countByTherapistIdAndStatusAndStartTimeBetween(
			String therapistId,
			AppointmentStatus status,
			LocalDateTime start,
			LocalDateTime end
			);

	/*
	 * BILLABLE = COMPLETED + ABANDONED (owner decision, 30 Jul). A no-show
	 * consumed the slot and the therapist's time, so it bills the full session
	 * fee. Safe to do purely in the query: AppointmentEventConsumer stamps
	 * sessionFee at booking (createAppointment / rescheduleAppointment) and
	 * updateStatus never clears it, so an ABANDONED row still carries its fee.
	 *
	 * Note this is retroactive by construction — earnings are derived live from
	 * projection rows by status, so every historical no-show starts counting the
	 * moment this ships.
	 *
	 * DSF stays COMPLETED-only below: it measures pro-bono work actually
	 * delivered, and it earns nothing either way.
	 *
	 * These queries deliberately do NOT join TherapistClients for the DSF flag.
	 * A pro-bono session is stamped with sessionFee = 0 at booking, so zero fee
	 * IS the pro-bono marker, and it is frozen per appointment. Filtering on the
	 * client's live flag instead would mean a therapist ending a pro-bono
	 * arrangement retroactively turned months of free sessions into income.
	 * Nothing else can produce a zero fee: ClientService rejects a negative rate
	 * and normalises zero to "no rate", and AppointmentService.resolveSessionFee
	 * returns zero only from its DSF branch — every other branch is checked for
	 * a positive amount before it returns.
	 */
	@Query("""
			SELECT SUM(a.sessionFee)
			FROM AppointmentProjection a
			WHERE a.therapistId = :therapistId
				AND a.status IN (
					com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED,
					com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
				)
				AND a.startTime >= :start
				AND a.startTime < :end
			""")
	BigDecimal sumPaidCompletedEarningsBetween(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end
			);

	@Query("""
			SELECT COUNT(a)
			FROM AppointmentProjection a
			WHERE a.therapistId = :therapistId
				AND a.status IN (
					com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED,
					com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
				)
				AND a.sessionFee > 0
				AND a.startTime >= :start
				AND a.startTime < :end
			""")
	long countPaidCompletedBetween(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end
			);

	/** No-shows that billed — surfaced separately so the therapist can see what
	 *  share of income came from sessions nobody attended. */
	@Query("""
			SELECT COUNT(a)
			FROM AppointmentProjection a
			WHERE a.therapistId = :therapistId
				AND a.status = com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
				AND a.sessionFee > 0
				AND a.startTime >= :start
				AND a.startTime < :end
			""")
	long countPaidAbandonedBetween(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end
			);

	@Query("""
			SELECT COUNT(a)
			FROM AppointmentProjection a
			WHERE a.therapistId = :therapistId
				AND a.status = com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED
				AND a.sessionFee = 0
				AND a.startTime >= :start
				AND a.startTime < :end
			""")
	long countDsfCompletedBetween(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end
			);

	@Query("""
			SELECT new com.org.therapistService.Dto.EarningsSessionDto(
				a.appointmentId,
				a.clientId,
				c.clientName,
				a.serviceId,
				a.modeId,
				a.startTime,
				a.endTime,
				a.sessionFee,
				c.dsf,
				a.status
			)
			FROM AppointmentProjection a
			JOIN TherapistClients c ON c.therapistId = a.therapistId AND c.clientId = a.clientId
			WHERE a.therapistId = :therapistId
				AND a.status IN (
					com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED,
					com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
				)
				AND a.startTime >= :start
				AND a.startTime < :end
				AND (:serviceId IS NULL OR a.serviceId = :serviceId)
				AND (:modeId IS NULL OR a.modeId = :modeId)
			ORDER BY a.startTime ASC
			""")
	List<EarningsSessionDto> findEarningsSessions(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end,
			String serviceId,
			String modeId
			);

	/**
	 * Per-client aggregates for the clients list, in one grouped pass.
	 *
	 * The LEFT JOIN onto SessionNotes cannot fan out: SessionNotes.appointmentId
	 * is unique, so each appointment matches at most one note row and COUNT(a)
	 * stays a true session count.
	 *
	 * Only COMPLETED appointments count — a booked future session is not a
	 * session had, and a session with no notes only becomes a backlog item once
	 * it has actually happened.
	 *
	 * TherapistClients is joined to carry the DSF flag on each row. Per the owner
	 * decision (29 Jul), a DSF client's pending notes are shown on that client's
	 * own page but must never feed the practice-wide "Notes due" total or the
	 * notification bell — so the caller sums pendingNotes only over non-DSF rows.
	 * dsf is functionally dependent on clientId, so grouping by both is safe.
	 */
	@Query("""
			SELECT new com.org.therapistService.Dto.ClientEnrichmentDto(
				a.clientId,
				COUNT(a),
				MAX(a.startTime),
				SUM(CASE WHEN n.noteId IS NULL THEN 1L ELSE 0L END),
				c.dsf
			)
			FROM AppointmentProjection a
			JOIN TherapistClients c ON c.therapistId = a.therapistId AND c.clientId = a.clientId
			LEFT JOIN SessionNotes n ON n.appointmentId = a.appointmentId
			WHERE a.therapistId = :therapistId
				AND a.status = com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED
			GROUP BY a.clientId, c.dsf
			""")
	List<ClientEnrichmentDto> findClientEnrichment(String therapistId);

	long countByTherapistIdAndStatusInAndStartTimeAfter(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime time
			);

	boolean existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime endTime,
			LocalDateTime startTime
			);

}
