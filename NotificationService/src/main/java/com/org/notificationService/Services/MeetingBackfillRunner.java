package com.org.notificationService.Services;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MeetingBackfillRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(MeetingBackfillRunner.class);
    private final MeetingBackfillService backfill;
    @Value("${meeting.backfill.csv:}") private String csv;
    @Value("${meeting.backfill.apply:false}") private boolean apply;
    public MeetingBackfillRunner(MeetingBackfillService backfill) { this.backfill=backfill; }
    public void run(ApplicationArguments args) throws Exception {
        if (csv.isBlank()) return;
        Map<String,Integer> counts=new LinkedHashMap<>();
        int processed=0;
        try (var lines=Files.lines(Path.of(csv))) {
            var iterator=lines.iterator();
            if (!iterator.hasNext() || !iterator.next().equals("appointment_id,therapist_id,calendar_revision,start_time,end_time"))
                throw new IllegalArgumentException("Unexpected meeting backfill CSV header");
            while(iterator.hasNext()) {
                String line=iterator.next(); if(line.isBlank()) continue;
                String[] row=line.split(",",-1);
                if(row.length!=5 || !row[0].matches("[A-Za-z0-9_-]+") || !row[1].matches("[A-Za-z0-9_-]+"))
                    throw new IllegalArgumentException("Invalid backfill row at line " + (processed+2));
                String outcome;
                try { outcome=backfill.inspect(row[0],row[1],Long.parseLong(row[2]),row[3],row[4],apply); }
                catch(Exception ex) { outcome="ERROR"; log.warn("Meeting backfill read failed appointmentId={}",row[0]); }
                counts.merge(outcome,1,Integer::sum); processed++;
                // Rate limited, read-only Google calls; each row has its own DB transaction.
                Thread.sleep(250);
            }
        }
        log.info("Meeting backfill apply={} rows={} results={}",apply,processed,counts);
    }
}
