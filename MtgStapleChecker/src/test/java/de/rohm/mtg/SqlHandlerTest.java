package de.rohm.mtg;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rohmio.mtg.DatabaseConfig;
import de.rohmio.mtg.io.IOHandler;
import de.rohmio.mtg.io.SqlHandler;
import de.rohmio.mtg.model.CardStapleInfo;

public class SqlHandlerTest {

	private IOHandler handler;

	@Before
	public void init() throws IOException {
		handler = new SqlHandler(DatabaseConfig.loadConfig());
		handler.init();
	}

	@Test
	public void addDatasetTest() throws IOException {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		System.out.println(cardStapleInfo);
//		handler.addDataset(cardStapleInfo);
	}

}
