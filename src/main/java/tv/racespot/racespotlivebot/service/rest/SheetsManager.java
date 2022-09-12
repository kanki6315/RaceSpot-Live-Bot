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
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Spreadsheet;
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

        Spreadsheet sheet = service.spreadsheets().get(spreadsheetId)
            .setIncludeGridData(true).setRanges(Collections.singletonList(defaultRange)).execute();
        GridData gridData = sheet.getSheets().get(0).getData().get(0);
        List<RowData> rowData = gridData.getRowData();

        /*ValueRange response = service.spreadsheets().values()
            .get(spreadsheetId, defaultRange)
            .execute();*/

        List<ScheduledEvent> entries = getEntriesFromSheetResponse(rowData);

        long lastUpdated = System.currentTimeMillis();
        logger.info(String.format("Updated schedule at %d", lastUpdated));
        return entries;
    }

    private List<ScheduledEvent> getEntriesFromSheetResponse(final List<RowData> response) throws IOException {

        List<ScheduledEvent> entries = new ArrayList<>();
        int index = 1;

        for (RowData data : response.subList(1, response.size())) {
            List<CellData> cells = data.getValues();

            if(cells == null || cells.size() < 14) {
                continue;
            }
            ScheduledEvent event = new ScheduledEvent();
            event.setDate(cells.get(MasterScheduleCSVInputs.DATE_REF).getFormattedValue());
            event.setTime(cells.get(MasterScheduleCSVInputs.UTC_REF).getFormattedValue());
            event.setPublic("yes".equalsIgnoreCase(cells.get(MasterScheduleCSVInputs.PUBLIC_REF).getFormattedValue()));
            event.setSeriesName(cells.get(MasterScheduleCSVInputs.SERIES_REF).getFormattedValue());
            event.setDescription(getFormattedValueOrDefault(cells, MasterScheduleCSVInputs.DESCRIPTION_REF));
            event.setProducer(cells.get(MasterScheduleCSVInputs.PROD_REF).getFormattedValue());
            event.setLeadCommentator(cells.get(MasterScheduleCSVInputs.COMM_1_REF).getFormattedValue());
            event.setColourOne(cells.get(MasterScheduleCSVInputs.COMM_2_REF).getFormattedValue());
            event.setColourTwo(cells.get(MasterScheduleCSVInputs.COMM_3_REF).getFormattedValue());
            event.setStreamLocation(cells.get(MasterScheduleCSVInputs.STREAMED_AT_REF).getFormattedValue());
            event.setWebcam("yes".equalsIgnoreCase(cells.get(MasterScheduleCSVInputs.ZOOM_REF).getFormattedValue()));
            event.setNotes(cells.get(MasterScheduleCSVInputs.NOTES_REF).getFormattedValue());
            event.setIndex(index);

            Color color = cells.get(MasterScheduleCSVInputs.SERIES_REF).getUserEnteredFormat().getBackgroundColor();
            if(color == null || color.size() == 0) {
                event.setRed(0f);
                event.setBlue(0f);
                event.setGreen(0f);
            } else {
                if (color.getBlue() == null) {
                    event.setBlue(0f);
                } else {
                    event.setBlue(color.getBlue());
                }
                if (color.getRed() == null) {
                    event.setRed(0f);
                } else {
                    event.setRed(color.getRed());
                }
                if (color.getGreen() == null) {
                    event.setGreen(0f);
                } else {
                    event.setGreen(color.getGreen());
                }
            }

            if(StringUtils.isNotEmpty(event.getSeriesName())) {
                entries.add(event);
            }
            index++;
        }

        return entries;
    }

    private String getFormattedValueOrDefault(List<CellData> cells, int csvHeader) {
        String returnData = cells.get(csvHeader).getFormattedValue();

        if(StringUtils.isEmpty(returnData)) {
            return "";
        }
        return returnData;
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