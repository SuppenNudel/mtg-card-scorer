package de.rohm.mtg;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rohmio.mtg.Config;
import de.rohmio.mtg.io.IOHandler;
import de.rohmio.mtg.io.SqlHandler;
import de.rohmio.mtg.model.CardStapleInfo;

public class SqlHandlerTest {
	
	private IOHandler handler;
	
	@Before
	public void init() throws IOException {
		Config config = Config.loadConfig();
		handler = new SqlHandler(
				config.getHost(),
				config.getPort(),
				config.getUser(),
				config.getPassword(),
				config.getDatabase(),
				config.getTable());
		handler.init();
	}
	
	@Test
	public void addDatasetTest() throws IOException {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		System.out.println(cardStapleInfo);
//		handler.addDataset(cardStapleInfo);
	}

}
