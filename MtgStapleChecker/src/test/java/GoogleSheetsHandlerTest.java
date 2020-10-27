import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.write.GoogleSheetsHandler;
import de.rohmio.mtg.write.IOHandler;

public class GoogleSheetsHandlerTest {
	
	private IOHandler handler;
	
	@Before
	public void init() throws IOException {
		handler = new GoogleSheetsHandler();
		handler.init(null);
	}
	
	@Test
	public void read() {
		List<CardStapleInfo> cardsNotNeededAnymore = handler.getCardsNotNeededAnymore(7);
	}

}
