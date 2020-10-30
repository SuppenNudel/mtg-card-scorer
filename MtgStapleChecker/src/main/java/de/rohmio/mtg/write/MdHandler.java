package de.rohmio.mtg.write;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import de.rohmio.mtg.model.CardStapleInfo;

public class MdHandler implements IOHandler {

	private static Logger log = Logger.getLogger(MdHandler.class.getName());

	private static final String ln = System.lineSeparator();
	private static final String valueSeparator = " | ";

	private File file;
	private List<String> fields;

	public MdHandler(File file) {
		this.file = file;
	}

	@Override
	public void init() throws IOException {
		fields = new ArrayList<>();
		fields.add(0, FIELD_CARDNAME);
		fields.add(1, FIELD_TIMESTAMP);
		
		log.info("init md file: " + file);
		log.info("deleting file: " + file);
		file.delete();

		String titlesLine = String.join(valueSeparator, fields);
		FileUtils.writeStringToFile(file, titlesLine + ln, "UTF-8", true);

		String hr = String.join("", Collections.nCopies(fields.size(), " | ---"));
		FileUtils.writeStringToFile(file, "---" + hr + ln, "UTF-8", true);
	}

	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) throws IOException {
		if(true) {
			throw new RuntimeException("Not implemented");
		}
		Map<String, String> values = new HashMap<>();
		StringBuilder sb_line = new StringBuilder();
		for (String title : fields) {
			String value = values.get(title);
			sb_line.append(valueSeparator);
			if (value != null) {
				sb_line.append(value);
			}
		}
		String escapePipe = valueSeparator.replace("|", "\\|");
		String line = sb_line.toString().replaceFirst(escapePipe, "");
		System.out.println(String.format("Writing in '%s': %s", file.getName(), line));
		FileUtils.writeStringToFile(file, line + ln, "UTF-8", true);
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
