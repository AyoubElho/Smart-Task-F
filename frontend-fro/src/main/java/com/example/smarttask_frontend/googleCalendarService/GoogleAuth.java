package com.example.smarttask_frontend.googleCalendarService;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.io.InputStreamReader;
import java.util.Collections;

public class GoogleAuth {

    public static Credential authorize() throws Exception {

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleClientSecrets secrets =
                GoogleClientSecrets.load(
                        jsonFactory,
                        new InputStreamReader(
                                GoogleAuth.class.getResourceAsStream("/credentials.json")
                        )
                );

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        jsonFactory,
                        secrets,
                        Collections.singleton(CalendarScopes.CALENDAR)
                )
                        .setAccessType("offline")
                        .build();

        return new AuthorizationCodeInstalledApp(
                flow,
                new LocalServerReceiver()
        ).authorize("user");
    }
}
