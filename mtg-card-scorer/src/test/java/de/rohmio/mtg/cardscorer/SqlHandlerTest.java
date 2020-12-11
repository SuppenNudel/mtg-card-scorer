package de.rohmio.mtg.cardscorer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.rohmio.mtg.cardscorer.config.DatabaseConfig;
import de.rohmio.mtg.cardscorer.io.IOHandler;
import de.rohmio.mtg.cardscorer.io.SqlHandler;
import de.rohmio.mtg.cardscorer.model.CardStapleInfo;
import de.rohmio.mtg.scryfall.api.model.Format;

public class SqlHandlerTest {

	private IOHandler handler;

	@BeforeEach
	public void init() throws IOException {
		DatabaseConfig db_config = DatabaseConfig.loadConfig("@database.config");
		handler = new SqlHandler(db_config);
	}

	@Test
	public void addDatasetTest() {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		cardStapleInfo.setFormatScore(Format.modern, 5158);
		System.out.println(cardStapleInfo);
		handler.addDataset(cardStapleInfo);
	}

	@Test
	public void requestMultipleCards() {
		List<String> cards = Arrays.asList("Lightning Bolt", "Underground Sea", "Lightning Strike");
		List<CardStapleInfo> cardStapleInfos = handler.getCardStapleInfos(cards);
		cardStapleInfos.forEach(System.out::println);
	}

	@Test
	public void requestSingleCards() {
		CardStapleInfo cardStapleInfos = handler.getCardStapleInfo("Lightning Bolt");
		System.out.println(cardStapleInfos);
	}
	
}
