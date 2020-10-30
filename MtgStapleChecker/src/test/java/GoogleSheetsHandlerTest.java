import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rohmio.mtg.io.GoogleSheetsHandler;
import de.rohmio.mtg.io.IOHandler;
import de.rohmio.mtg.model.CardStapleInfo;

public class GoogleSheetsHandlerTest {
	
	private IOHandler handler;
	
	@Before
	public void init() throws IOException {
		handler = new GoogleSheetsHandler();
		handler.init();
	}
	
	@Test
	public void read() {
		List<CardStapleInfo> cardsNotNeededAnymore = handler.getCardsNotNeeded(7);
	}

}
