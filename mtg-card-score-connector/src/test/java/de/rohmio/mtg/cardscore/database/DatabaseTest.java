package de.rohmio.mtg.cardscore.database;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;

public class DatabaseTest {

	private StorageConnector connector;

	@BeforeEach
	public void init() {
		DatabaseConfig config = DatabaseConfig.loadConfig("@database.config");
		connector = new SqlConnector(config);
	}

	@Test
	public void writeDatabase() {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("Test Card");
		cardStapleInfo.setFormatScore(MtgTop8Format.LEGACY, 50);
		cardStapleInfo.setTimestamp(LocalDateTime.now());
		connector.addDataset(cardStapleInfo);
	}

	@Test
	public void readDatabase() {
		CardStapleInfo cardStapleInfo = connector.getCardStapleInfo("TEST CARD");
		System.out.println(cardStapleInfo);
	}

}
