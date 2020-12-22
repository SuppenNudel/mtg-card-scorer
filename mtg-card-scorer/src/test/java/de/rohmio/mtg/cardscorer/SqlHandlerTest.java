package de.rohmio.mtg.cardscorer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;
import de.rohmio.mtg.cardscore.database.CardStapleInfo;
import de.rohmio.mtg.cardscore.database.DatabaseConfig;
import de.rohmio.mtg.cardscore.database.SqlConnector;
import de.rohmio.mtg.cardscore.database.StorageConnector;

public class SqlHandlerTest {

	private StorageConnector handler;

	@BeforeEach
	public void init() throws IOException {
		DatabaseConfig db_config = DatabaseConfig.loadConfig("@database.config");
		handler = new SqlConnector(db_config);
	}

	@Test
	public void addDatasetTest() {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		cardStapleInfo.setFormatScore(MtgTop8Format.MODERN, 5158);
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
