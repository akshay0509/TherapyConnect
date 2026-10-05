package com.org.appointmentService.Meeting;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapyDeliveryMode;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class MeetingQueryTest {
    @Test void hibernateValidatesConditionalProjectionQueryWithoutDatabase() throws Exception {
        var config=new Configuration().addAnnotatedClass(TherapistAppointments.class).addAnnotatedClass(TherapyDeliveryMode.class);
        config.setProperty("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.boot.allow_jdbc_metadata_access","false");
        config.setProperty("hibernate.hbm2ddl.auto","none");
        config.setProperty("hibernate.connection.provider_class","org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl");
        var method=TherapistAppointmentsRepository.class.getMethod("applyMeetingUpdate",String.class,String.class,
            long.class,long.class,String.class,String.class,boolean.class);
        try(var factory=config.buildSessionFactory(); var session=factory.openSession()) {
            var query=session.createMutationQuery(method.getAnnotation(Query.class).value());
            query.setParameter("appointmentId","a1").setParameter("therapistId","t1").setParameter("revision",1L)
                .setParameter("sequence",2L).setParameter("meetingStatus","READY")
                .setParameter("url","https://meet.google.com/abc-defg-hij").setParameter("eligible",true);
        }
    }
}
