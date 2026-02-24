package com.org.notificationService.Config;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

@Configuration
public class GoogleCalendarConfig {

	private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    @Value("${google.calendar.credentials-path}")
    private String credentialsPath;

    @Value("${google.calendar.tokens-directory}")
    private String tokensDirectory;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    @Bean
    public Calendar googleCalendar() throws Exception {

        // Transport (thread-safe)
        final NetHttpTransport httpTransport =
                GoogleNetHttpTransport.newTrustedTransport();

        // Load client secrets using GsonFactory (NOT JacksonFactory)
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY,
                        new FileReader(credentialsPath)
                );

        // Build authorization flow
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        List.of(CalendarScopes.CALENDAR)
                )
                .setDataStoreFactory(
                        new FileDataStoreFactory(
                                new File(tokensDirectory)
                        )
                )
                .setAccessType("offline")
                .setApprovalPrompt("force") // ensures refresh token
                .build();

        // Local receiver for OAuth callback
        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setHost("localhost")
                        .setPort(8888)
                        .build();

        // Authorize
        Credential credential =
                new AuthorizationCodeInstalledApp(flow, receiver)
                        .authorize("platform-user");

        // Build Calendar client
        return new Calendar.Builder(
                httpTransport,
                JSON_FACTORY,
                credential
        )
        .setApplicationName(applicationName)
        .build();
    }
}
