/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import tv.racespot.racespotlivebot.data.ScheduledEvent;
import tv.racespot.racespotlivebot.util.MasterScheduleCSVInputs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsManager {

    private Logger logger;

    private final String spreadsheetId;
    private final int gid;
    private final String defaultRange;

    private Credential credential;

    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public SheetsManager(
        final String spreadsheetId,
        final int gid,
        final String defaultRange) {
        this.spreadsheetId = spreadsheetId;
        this.gid = gid;
        this.defaultRange = defaultRange;

        this.logger = LoggerFactory.getLogger(SheetsManager.class);
        ;

        connectAndCacheToken();
    }

    public void updateAttendance(ScheduledEvent event, boolean isAttending, String talentName)
        throws IOException, GeneralSecurityException {

        List<Request> requestList = new ArrayList<>();

        if (event.getProducer().equalsIgnoreCase(talentName)) {
            requestList.add(getUpdateRequest(event, isAttending, 8));
        }
        if (event.getLeadCommentator().equalsIgnoreCase(talentName)) {
            requestList.add(getUpdateRequest(event, isAttending, 9));
        }
        if (StringUtils.isNotEmpty(event.getColourOne()) && event.getColourOne()
            .equalsIgnoreCase(talentName)) {
            requestList.add(getUpdateRequest(event, isAttending, 10));
        }
        if (StringUtils.isNotEmpty(event.getColourTwo()) && event.getColourTwo()
            .equalsIgnoreCase(talentName)) {
            requestList.add(getUpdateRequest(event, isAttending, 11));
        }

        if (requestList.size() > 0) {
            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
            batchUpdateSpreadsheetRequest.setRequests(requestList);

            final Sheets.Spreadsheets.BatchUpdate batchUpdate = getSheetService().
                spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest);

            batchUpdate.execute();
        }
    }

    private Request getUpdateRequest(ScheduledEvent event, boolean isAttending, int colIndex) {
        CellFormat backgroundColor = new CellFormat();
        backgroundColor.setBackgroundColor(getColor(isAttending));

        CellData cellData = new CellData();
        cellData.setUserEnteredFormat(backgroundColor);

        GridRange gridRange = new GridRange();
        gridRange.setSheetId(gid);
        gridRange.setStartRowIndex(event.getIndex());
        gridRange.setEndRowIndex(event.getIndex() + 1);
        gridRange.setStartColumnIndex(colIndex);
        gridRange.setEndColumnIndex(colIndex + 1);

        return new Request().setRepeatCell(new RepeatCellRequest()
            .setCell(cellData).setRange(gridRange).setFields("userEnteredFormat.backgroundColor"));
    }

    private Color getColor(boolean isAttending) {
        Color color = new Color();

        if (isAttending) {
            color.setRed(0f);
            color.setGreen(1f);
            color.setBlue(0f);
        } else {
            color.setRed(1f);
            color.setGreen(0f);
            color.setBlue(0f);
        }
        return color;
    }

    public List<ScheduledEvent> getWeeklyEvents() throws IOException, GeneralSecurityException {

        logger.info("Updating weekly schedule");
        Sheets service = getSheetService();

        ValueRange response = service.spreadsheets().values()
            .get(spreadsheetId, defaultRange)
            .execute();

        List<ScheduledEvent> entries = getEntriesFromSheetResponse(response);

        long lastUpdated = System.currentTimeMillis();
        logger.info(String.format("Updated schedule at %d", lastUpdated));
        return entries;
    }

    private List<ScheduledEvent> getEntriesFromSheetResponse(final ValueRange response) throws IOException {
        List<List<Object>> spreadsheetOutput = response.getValues();
        List<String> headers = spreadsheetOutput.get(0).stream()
            .map(object -> Objects.toString(object, null))
            .collect(Collectors.toList());

        String csv = getCsvFromValues(spreadsheetOutput.subList(1, spreadsheetOutput.size()));

        CSVParser csvParser = new CSVParser(
            new StringReader(csv),
            CSVFormat.DEFAULT
                .withHeader(headers.toArray(new String[headers.size()]))
                .withDelimiter(',')
                .withIgnoreHeaderCase()
                .withIgnoreEmptyLines()
                .withTrim());

        List<ScheduledEvent> entries = new ArrayList<>();
        int i = 1; // start at 1 for header row in GSheets
        for (CSVRecord record : csvParser) {
            entries.add(getEntryFromRecord(record, i));
            i++;
        }

        return entries;
    }

    private ScheduledEvent getEntryFromRecord(final CSVRecord csvRecord, int index) {
        Map<String, String> record = csvRecord.toMap();
        ScheduledEvent event = new ScheduledEvent();
        event.setDate(record.get(MasterScheduleCSVInputs.DATE));
        event.setTime(record.get(MasterScheduleCSVInputs.UTC));
        event.setPublic("yes".equalsIgnoreCase(record.get(MasterScheduleCSVInputs.PUBLIC)));
        event.setSeriesName(record.get(MasterScheduleCSVInputs.SERIES));
        event.setDescription(record.get(MasterScheduleCSVInputs.DESCRIPTION));
        event.setProducer(record.get(MasterScheduleCSVInputs.PROD));
        event.setLeadCommentator(record.get(MasterScheduleCSVInputs.COMM_1));
        event.setColourOne(record.get(MasterScheduleCSVInputs.COMM_2));
        event.setColourTwo(record.get(MasterScheduleCSVInputs.COMM_3));
        event.setStreamLocation(record.get(MasterScheduleCSVInputs.STREAMED_AT));
        event.setWebcam("yes".equalsIgnoreCase(record.get(MasterScheduleCSVInputs.ZOOM)));
        event.setNotes(record.get(MasterScheduleCSVInputs.NOTES));
        event.setIndex(index);
        return event;
    }

    private String getCsvFromValues(List<List<Object>> subList) {

        StringJoiner csvBuilder = new StringJoiner("\n");
        for (List<Object> row : subList) {
            StringJoiner rowBuilder = new StringJoiner(",");
            for (Object item : row) {
                rowBuilder.add(String.format("\"%s\"", item.toString()));
            }
            csvBuilder.add(rowBuilder.toString());
        }

        return csvBuilder.toString();
    }

    private void connectAndCacheToken() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            credential = getCredentials(HTTP_TRANSPORT);
            logger.info("Cached token for GSheets");
        } catch (IOException | GeneralSecurityException ex) {
            logger.info("Unable to connect/authorize with sheets", ex);
        }
    }

    private Sheets getSheetService() throws GeneralSecurityException, IOException {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName("RaceSpotTV Live Bot")
            .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

        final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
        logger.info(Paths.get("").toString());

        // Load client secrets.
        InputStream in = SheetsManager.class.getResourceAsStream("/credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        File file = new File("tokens");
        logger.info(file.getCanonicalPath());
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(file))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}