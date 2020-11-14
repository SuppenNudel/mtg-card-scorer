package de.rohmio.mtg;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.rohmio.mtg.cardscorer.config.DatabaseConfig;
import de.rohmio.mtg.cardscorer.io.IOHandler;
import de.rohmio.mtg.cardscorer.io.SqlHandler;
import de.rohmio.mtg.cardscorer.model.CardStapleInfo;

public class SqlHandlerTest {

	private IOHandler handler;

	@BeforeEach
	public void init() throws IOException {
		DatabaseConfig db_config = new DatabaseConfig();
		handler = new SqlHandler(db_config);
		handler.init();
	}

	@Test
	public void addDatasetTest() throws IOException {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		System.out.println(cardStapleInfo);
//		handler.addDataset(cardStapleInfo);
	}

}
