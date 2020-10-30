package de.rohmio.mtg.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import com.google.api.services.sheets.v4.model.ValueRange;

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.scryfall.api.model.enums.Format;

public class GoogleSheetsHandler implements IOHandler {
	
	private static final String APPLICATION_NAME = "Mtg Staple Checker";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	
    private static final String spreadsheetId = "17ZRWv6ith2ewTntSmysnxZZ0aa_7f8B_9upne8srIkU";
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	
	private Sheets service;
	
	@Override
	public void init() throws IOException {
        // Build a new authorized API client service.
		try {
			NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleSheetsHandler.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) throws IOException {
		String timestamp = dateFormat.format(new Date());
		List<List<Object>> writeValues = Arrays.asList(
				Arrays.asList(
						cardStapleInfo.getCardname(),
						cardStapleInfo.getFormatScore(Format.pioneer),
						cardStapleInfo.getFormatScore(Format.modern),
						cardStapleInfo.getFormatScore(Format.legacy),
						timestamp
				)
				// Additional rows ...
		);
		ValueRange body = new ValueRange()
			.setValues(writeValues);
		service.spreadsheets().values().append(spreadsheetId, "Scores", body)
				.setValueInputOption("USER_ENTERED")
				.execute();
		System.out.println("Added dataset to spreadsheet: "+cardStapleInfo);
	}

	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<CardStapleInfo> getCardsNotNeeded(int daysAgo) {
		List<CardStapleInfo> cardStapleInfos = new ArrayList<>();
		try {
			ValueRange result = service.spreadsheets().values().get(spreadsheetId, "A1:E10000").execute();
			List<List<Object>> values = result.getValues();
			values.remove(0);

			Calendar then = Calendar.getInstance();
			Calendar threshold = Calendar.getInstance();
			threshold.add(Calendar.DAY_OF_YEAR, -daysAgo);
			for(List<Object> row : values) {
				String cardname = (String) row.get(0);
				String timestamp = (String) row.get(4);
				try {
					Date parsed = dateFormat.parse(timestamp);
					then.setTime(parsed);
					if(then.after(threshold)) {
						cardStapleInfos.add(new CardStapleInfo(cardname));
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cardStapleInfos;
	}
	
}
