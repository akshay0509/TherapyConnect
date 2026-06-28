package com.org.analyticsService.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.analyticsService.Dto.AnalyticsSummaryDto;
import com.org.analyticsService.Dto.DailySnapshotDto;
import com.org.analyticsService.Dto.RetentionSummaryDto;
import com.org.analyticsService.Dto.ServiceBreakdownDto;
import com.org.analyticsService.Dto.SessionFrequencyDto;
import com.org.analyticsService.Entity.AnalyticsDaily;
import com.org.analyticsService.Entity.AnalyticsDailyId;
import com.org.analyticsService.Entity.AnalyticsServiceDaily;
import com.org.analyticsService.Entity.AnalyticsServiceDailyId;
import com.org.analyticsService.Entity.ClientEngagementProjection;
import com.org.analyticsService.Repository.AnalyticsDailyRepository;
import com.org.analyticsService.Repository.AnalyticsServiceDailyRepository;
import com.org.analyticsService.Repository.ClientDsfProjectionRepository;
import com.org.analyticsService.Repository.ClientEngagementRepository;
import com.org.events.TherapistAppointment.AppointmentEvent;

@Service
public class AnalyticsAggregationService {

    @Autowired
    private AnalyticsDailyRepository dailyRepository;

    @Autowired
    private AnalyticsServiceDailyRepository serviceDailyRepository;

    @Autowired
    private ClientDsfProjectionRepository clientDsfProjectionRepository;

    @Autowired
    private ClientEngagementRepository clientEngagementRepository;

    // ── Event handlers ────────────────────────────────────────────────────────

    public void handleCompleted(AppointmentEvent event) {
        LocalDate date = event.getStartTime().toLocalDate();
        String therapistId = event.getTherapistId();

        boolean dsf = clientDsfProjectionRepository
                .findByClientIdAndTherapistId(event.getClientId(), therapistId)
                .map(p -> p.isDsf())
                .orElse(false);

        BigDecimal earnings = dsf ? BigDecimal.ZERO
                : (event.getSessionFee() != null ? event.getSessionFee() : BigDecimal.ZERO);

        boolean online = "ONLINE".equals(event.getModeType());

        AnalyticsDaily daily = findOrCreateDaily(therapistId, date);
        daily.setCompletedCount(daily.getCompletedCount() + 1);
        daily.setEarnings(daily.getEarnings().add(earnings));
        if (dsf) daily.setDsfCount(daily.getDsfCount() + 1);
        else daily.setPaidCount(daily.getPaidCount() + 1);
        if (online) daily.setOnlineCount(daily.getOnlineCount() + 1);
        else daily.setOfflineCount(daily.getOfflineCount() + 1);
        dailyRepository.save(daily);

        if (event.getServiceId() != null) {
            AnalyticsServiceDaily svcDaily = findOrCreateServiceDaily(therapistId, date, event.getServiceId());
            svcDaily.setCompletedCount(svcDaily.getCompletedCount() + 1);
            svcDaily.setEarnings(svcDaily.getEarnings().add(earnings));
            serviceDailyRepository.save(svcDaily);
        }

        // Track per-client engagement for retention analytics
        ClientEngagementProjection engagement = clientEngagementRepository
                .findByClientIdAndTherapistId(event.getClientId(), therapistId)
                .orElseGet(() -> new ClientEngagementProjection(event.getClientId(), therapistId, date));
        engagement.setLastSessionDate(date);
        engagement.setTotalSessions(engagement.getTotalSessions() + 1);
        clientEngagementRepository.save(engagement);
    }

    public void handleCancelled(AppointmentEvent event) {
        LocalDate date = event.getStartTime().toLocalDate();
        AnalyticsDaily daily = findOrCreateDaily(event.getTherapistId(), date);
        daily.setCancelledCount(daily.getCancelledCount() + 1);
        dailyRepository.save(daily);
    }

    public void handleAbandoned(AppointmentEvent event) {
        LocalDate date = event.getStartTime().toLocalDate();
        AnalyticsDaily daily = findOrCreateDaily(event.getTherapistId(), date);
        daily.setAbandonedCount(daily.getAbandonedCount() + 1);
        dailyRepository.save(daily);
    }

    public void handleRescheduled(AppointmentEvent event) {
        // Count the reschedule against the original session date
        LocalDate date = event.getOldStartTime() != null
                ? event.getOldStartTime().toLocalDate()
                : event.getStartTime().toLocalDate();
        AnalyticsDaily daily = findOrCreateDaily(event.getTherapistId(), date);
        daily.setRescheduledCount(daily.getRescheduledCount() + 1);
        dailyRepository.save(daily);
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    public List<DailySnapshotDto> getDailySnapshots(String therapistId, LocalDate from, LocalDate to) {
        return dailyRepository.findByTherapistIdAndDateBetweenOrderByDateAsc(therapistId, from, to)
                .stream()
                .map(this::toSnapshotDto)
                .toList();
    }

    public AnalyticsSummaryDto getSummary(String therapistId, LocalDate from, LocalDate to) {
        List<AnalyticsDaily> rows = dailyRepository.findByTherapistIdAndDateBetweenOrderByDateAsc(therapistId, from, to);

        int totalCompleted   = rows.stream().mapToInt(AnalyticsDaily::getCompletedCount).sum();
        int totalCancelled   = rows.stream().mapToInt(AnalyticsDaily::getCancelledCount).sum();
        int totalAbandoned   = rows.stream().mapToInt(AnalyticsDaily::getAbandonedCount).sum();
        int totalRescheduled = rows.stream().mapToInt(AnalyticsDaily::getRescheduledCount).sum();
        int totalPaid        = rows.stream().mapToInt(AnalyticsDaily::getPaidCount).sum();
        int totalDsf         = rows.stream().mapToInt(AnalyticsDaily::getDsfCount).sum();
        int totalOnline      = rows.stream().mapToInt(AnalyticsDaily::getOnlineCount).sum();
        int totalOffline     = rows.stream().mapToInt(AnalyticsDaily::getOfflineCount).sum();
        BigDecimal totalEarnings = rows.stream()
                .map(AnalyticsDaily::getEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalAttempted = totalCompleted + totalCancelled + totalAbandoned;
        double completionRate   = totalAttempted > 0 ? (double) totalCompleted / totalAttempted * 100 : 0;
        double cancellationRate = totalAttempted > 0 ? (double) totalCancelled / totalAttempted * 100 : 0;

        return new AnalyticsSummaryDto(
                totalCompleted, totalCancelled, totalAbandoned, totalRescheduled,
                totalPaid, totalDsf, totalOnline, totalOffline,
                totalEarnings, completionRate, cancellationRate);
    }

    public List<ServiceBreakdownDto> getServiceBreakdown(String therapistId, LocalDate from, LocalDate to) {
        List<AnalyticsServiceDaily> rows = serviceDailyRepository.findByTherapistIdAndDateBetween(therapistId, from, to);

        Map<String, Integer> countByService = rows.stream().collect(
                Collectors.groupingBy(AnalyticsServiceDaily::getServiceId,
                        Collectors.summingInt(AnalyticsServiceDaily::getCompletedCount)));

        Map<String, BigDecimal> earningsByService = rows.stream().collect(
                Collectors.groupingBy(AnalyticsServiceDaily::getServiceId,
                        Collectors.reducing(BigDecimal.ZERO, AnalyticsServiceDaily::getEarnings, BigDecimal::add)));

        return countByService.entrySet().stream()
                .map(e -> new ServiceBreakdownDto(
                        e.getKey(),
                        e.getValue(),
                        earningsByService.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .toList();
    }

    public RetentionSummaryDto getRetentionSummary(String therapistId) {
        List<ClientEngagementProjection> clients = clientEngagementRepository.findByTherapistId(therapistId);

        int totalUniqueClients = clients.size();
        if (totalUniqueClients == 0) {
            return new RetentionSummaryDto(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int retainedClients = (int) clients.stream()
                .filter(c -> c.getTotalSessions() > 1)
                .count();

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        int churnedClients = (int) clients.stream()
                .filter(c -> c.getLastSessionDate() != null && c.getLastSessionDate().isBefore(thirtyDaysAgo))
                .count();

        double retentionRate = (double) retainedClients / totalUniqueClients * 100;

        double avgSessionsPerClient = clients.stream()
                .mapToInt(ClientEngagementProjection::getTotalSessions)
                .average()
                .orElse(0.0);

        // Average days between first and last session, only for clients with multiple sessions
        double avgClientLifetimeDays = clients.stream()
                .filter(c -> c.getFirstSessionDate() != null && c.getLastSessionDate() != null
                        && !c.getFirstSessionDate().equals(c.getLastSessionDate()))
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getFirstSessionDate(), c.getLastSessionDate()))
                .average()
                .orElse(0.0);

        return new RetentionSummaryDto(
                totalUniqueClients, retainedClients, churnedClients,
                retentionRate, avgSessionsPerClient, avgClientLifetimeDays);
    }

    public List<SessionFrequencyDto> getSessionFrequency(String therapistId) {
        List<ClientEngagementProjection> clients = clientEngagementRepository.findByTherapistId(therapistId);

        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("1",    clients.stream().filter(c -> c.getTotalSessions() == 1).count());
        buckets.put("2-3",  clients.stream().filter(c -> c.getTotalSessions() >= 2  && c.getTotalSessions() <= 3).count());
        buckets.put("4-6",  clients.stream().filter(c -> c.getTotalSessions() >= 4  && c.getTotalSessions() <= 6).count());
        buckets.put("7-10", clients.stream().filter(c -> c.getTotalSessions() >= 7  && c.getTotalSessions() <= 10).count());
        buckets.put("10+",  clients.stream().filter(c -> c.getTotalSessions() > 10).count());

        return buckets.entrySet().stream()
                .map(e -> new SessionFrequencyDto(e.getKey(), (int) e.getValue().longValue()))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AnalyticsDaily findOrCreateDaily(String therapistId, LocalDate date) {
        return dailyRepository.findById(new AnalyticsDailyId(therapistId, date))
                .orElseGet(() -> new AnalyticsDaily(therapistId, date));
    }

    private AnalyticsServiceDaily findOrCreateServiceDaily(String therapistId, LocalDate date, String serviceId) {
        return serviceDailyRepository.findById(new AnalyticsServiceDailyId(therapistId, date, serviceId))
                .orElseGet(() -> new AnalyticsServiceDaily(therapistId, date, serviceId));
    }

    private DailySnapshotDto toSnapshotDto(AnalyticsDaily a) {
        return new DailySnapshotDto(
                a.getDate(),
                a.getCompletedCount(),
                a.getCancelledCount(),
                a.getAbandonedCount(),
                a.getRescheduledCount(),
                a.getPaidCount(),
                a.getDsfCount(),
                a.getOnlineCount(),
                a.getOfflineCount(),
                a.getEarnings());
    }
}
