package de.rohmio.mtg;

import java.io.IOException;

import org.junit.Test;

public class RequestTest {
	
	@Test
	public void requestDecks() throws IOException {
		CompLevel[] compLevels = { CompLevel.Professional, CompLevel.Major };
		String card = "Leyline of the Void";
		MtgTop8Search mtgTop8Search = new MtgTop8Search();
		mtgTop8Search.getDecksContainingCard("MO", true, true, 90, card, compLevels);
	}

}
