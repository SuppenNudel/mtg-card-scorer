package de.rohmio.mtg.write;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.rohmio.mtg.MtgStapleChecker;
import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.scryfall.api.model.enums.Format;

public class CsvHandler implements IOHandler {

	private static final String ln = System.lineSeparator();
	private static final String valueSeparator = ",";

	private File file;
	private List<String> titles;
	
	private Map<String, CardStapleInfo> data = new HashMap<>();

	public CsvHandler(File file) {
		this.file = file;
	}

	@Override
	public void init(List<String> titles) throws IOException {
		this.titles = titles;
		if(file.exists()) {
			// load data
			String string = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			String[] lines = string.split(ln);
			
			String[] columns = null;
			for(int rowIdx=0; rowIdx<lines.length; ++rowIdx) {
				String line = lines[rowIdx];
				String[] values = line.split(",");
				if(rowIdx == 0) {
					columns = values;
				} else {
					for(int colIdx=0; colIdx<values.length; ++colIdx) {
						String key = columns[colIdx];
						String value = values[colIdx];
						Format format = Format.valueOf(key);
						System.out.println(value);
						System.out.println(format);
					}
				}
			}
			return;
		}

		String titlesLine = String.join(valueSeparator, titles);
		FileUtils.writeStringToFile(file, titlesLine + ln, "UTF-8", true);
	}

	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) throws IOException {
		Map<String, String> values = new HashMap<>();
		values.put(MtgStapleChecker.FIELD_CARDNAME, cardStapleInfo.getCardname());
		for(Format format : cardStapleInfo.getFormatScores().keySet()) {
			values.put(format.name(), String.valueOf(cardStapleInfo.getFormatScore(format)));
		}
		values.put(MtgStapleChecker.FIELD_TIMESTAMP, Calendar.getInstance().getTime().toString());
		StringBuilder sb_line = new StringBuilder();
		for (String title : titles) {
			String value = values.get(title);
			sb_line.append(valueSeparator);
			if (value != null) {
				// escape comma
				value = value.replace(",", "");
				sb_line.append(value);
			}
		}
		String line = sb_line.toString().replaceFirst(valueSeparator, "");
		System.out.println(String.format("Writing in '%s': %s", file.getName(), sb_line));
		FileUtils.writeStringToFile(file, line + ln, "UTF-8", true);
		
		data.put(values.get(MtgStapleChecker.FIELD_CARDNAME), cardStapleInfo);
	}

	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public List<CardStapleInfo> getCardsNotNeededAnymore(int daysAgo) {
		throw new RuntimeException("Not yet implemented");
	}
}
