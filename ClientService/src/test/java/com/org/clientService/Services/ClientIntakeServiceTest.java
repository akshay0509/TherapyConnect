package com.org.clientService.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.org.clientService.Dto.ApproveIntakeRequest;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Dto.IntakeClientData;
import com.org.clientService.Dto.IntakeDecisionResponse;
import com.org.clientService.Dto.RejectIntakeRequest;
import com.org.clientService.Entity.ClientIntake;
import com.org.clientService.Entity.ClientIntake.Status;
import com.org.clientService.Repository.ClientIntakeRepository;
import java.math.BigDecimal;

class ClientIntakeServiceTest {

    @Test
    void approvalAlwaysCreatesANewClient() throws Exception {
        ClientIntake intake = pendingIntake();
        RecordingClientService clientService = new RecordingClientService();
        ClientIntakeService service = service(intake, clientService);

        IntakeClientData reviewed = new IntakeClientData(
                "Fresh Client", null, null, Date.valueOf("1990-01-10"),
                "they/them", "Non-binary", "Masters", "Designer",
                "9000000000", "fresh@example.com", "Monday", "Evening", "Online",
                "Emergency Contact", 50, "Parent", "9111111111", true,
                new BigDecimal("1500"), Boolean.FALSE);

        IntakeDecisionResponse response = service.approve(
                "therapist-a", "user-a", "INT1", new ApproveIntakeRequest(reviewed));

        assertEquals(Status.APPROVED, response.status());
        assertEquals("CLT1", response.clientId());
        assertEquals(1, clientService.createCount);
        assertEquals("Fresh Client", clientService.created.getFullName());
        assertEquals("GOOGLE_FORM", clientService.created.getSource());
        assertEquals(new BigDecimal("1500"), clientService.created.getSessionFee());
    }

    /**
     * sessionFee == 0 is the app's one meaning of pro bono, and resolveSessionFee
     * decides it from the dsf flag when a session is actually booked. Storing a
     * rate next to the flag would be a second answer to the same question that
     * nothing ever reads — and that goes stale silently. So the fee is dropped
     * here even when the request carries one.
     */
    @Test
    void proBonoApprovalStoresNoFeeEvenIfOneWasSent() throws Exception {
        ClientIntake intake = pendingIntake();
        RecordingClientService clientService = new RecordingClientService();
        ClientIntakeService service = service(intake, clientService);

        IntakeClientData reviewed = new IntakeClientData(
                "Pro Bono Client", null, null, Date.valueOf("1990-01-10"),
                null, null, null, null,
                "9000000000", "probono@example.com", null, null, null,
                null, null, null, null, true,
                new BigDecimal("1500"), Boolean.TRUE);

        service.approve("therapist-a", "user-a", "INT1", new ApproveIntakeRequest(reviewed));

        assertNull(clientService.created.getSessionFee());
        assertEquals(Boolean.TRUE, clientService.created.getDsf());
    }

    @Test
    void rejectionNeverCreatesAClient() throws Exception {
        ClientIntake intake = pendingIntake();
        RecordingClientService clientService = new RecordingClientService();
        ClientIntakeService service = service(intake, clientService);

        IntakeDecisionResponse response = service.reject(
                "therapist-a", "user-a", "INT1", new RejectIntakeRequest("Not proceeding"));

        assertEquals(Status.REJECTED, response.status());
        assertEquals(0, clientService.createCount);
    }

    private ClientIntake pendingIntake() {
        ClientIntake intake = new ClientIntake();
        intake.setIntakeId("INT1");
        intake.setTherapistId("therapist-a");
        intake.setStatus(Status.PENDING);
        intake.setConsent(true);
        intake.setFullName("Fresh Client");
        intake.setDob(Date.valueOf("1990-01-10"));
        return intake;
    }

    private ClientIntakeService service(ClientIntake intake, ClientService clientService) {
        ClientIntakeRepository repository = (ClientIntakeRepository) Proxy.newProxyInstance(
                ClientIntakeRepository.class.getClassLoader(),
                new Class<?>[] {ClientIntakeRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByTherapistIdAndIntakeId" -> Optional.of(intake);
                    case "save" -> args[0];
                    case "toString" -> "ClientIntakeRepositoryTestDouble";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new ClientIntakeService(repository, clientService, "integration-a", "therapist-a");
    }

    private static class RecordingClientService extends ClientService {
        private int createCount;
        private ClientDto created;

        @Override
        public String createClient(ClientDto clientDto) {
            createCount++;
            created = clientDto;
            return "CLT1";
        }
    }
}
