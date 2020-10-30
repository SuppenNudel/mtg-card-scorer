package de.rohm.mtg;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rohmio.mtg.Config;
import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.write.IOHandler;
import de.rohmio.mtg.write.SqlHandler;

public class SqlHandlerTest {
	
	private IOHandler handler;
	
	@Before
	public void inti() {
		Config config = Config.loadConfig();
		handler = new SqlHandler(
				config.getHost(),
				config.getPort(),
				config.getUser(),
				config.getPassword(),
				config.getDatabase(),
				config.getTable());
	}
	
	@Test
	public void addDatasetTest() throws IOException {
		CardStapleInfo cardStapleInfo = new CardStapleInfo("TEST CARD");
		handler.addDataset(cardStapleInfo);
	}

}
