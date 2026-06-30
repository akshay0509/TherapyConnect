package com.org.notificationService.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

@Configuration
public class GoogleCalendarConfig {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.refresh-token}")
    private String refreshToken;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    @Bean
    public Calendar googleCalendar() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
