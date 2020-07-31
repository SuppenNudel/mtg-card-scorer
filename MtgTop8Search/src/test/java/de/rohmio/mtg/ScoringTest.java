package de.rohmio.mtg;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ScoringTest {
	
	private MtgTop8Search mtgTop8Search;
	
	@Before
	public void generateBase() {
		mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setStartDate("25/05/2019");
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setBoard(true, true);
		mtgTop8Search.setCards("Lightning Bolt");
	}
	
	@Test
	public void compareScoringMethods() throws IOException {
		mtgTop8Search.setFormat(MtgTop8Format.MO);

		// all
//		System.out.println(calculateScore(mtgTop8Search.getDecks()));
		
		// up to prof
		mtgTop8Search.setCompLevel(CompLevel.Professional);
		Assert.assertEquals(5, mtgTop8Search.getDeckCount());
		
		// up to major
		mtgTop8Search.setCompLevel(CompLevel.Major);
		Assert.assertEquals(223, mtgTop8Search.getDeckCount());

		// up to competitive
		mtgTop8Search.setCompLevel(CompLevel.Competitive);
		Assert.assertEquals(815, mtgTop8Search.getDeckCount());
		
		// gain all
		mtgTop8Search.setCompLevel(CompLevel.Regular);
		Assert.assertEquals(1148, mtgTop8Search.getDeckCount());
	}

	@Test
	public void allFormats() throws IOException {
		int allDecks = 0;
		for(MtgTop8Format format : MtgTop8Format.values()) {
			mtgTop8Search.setFormat(format);
			int decks = mtgTop8Search.getDeckCount();
			allDecks += decks;
		}
		int decks = mtgTop8Search.getDeckCount();
		
		Assert.assertEquals(allDecks, decks);
	}
	
	@Test
	public void requestDecks() throws IOException, InterruptedException {
		mtgTop8Search.setFormat(MtgTop8Format.MO);

		mtgTop8Search.setCards("Lightning Bolt");
		int decks1 = mtgTop8Search.getDeckCount();
		Assert.assertEquals(2191, decks1);

		
		mtgTop8Search.setCards("Birds of Paradise");
		int decks2 = mtgTop8Search.getDeckCount();
		Assert.assertEquals(415, decks2);
		
		
		mtgTop8Search.setCards("Leyline of the Void");
		int decks3 = mtgTop8Search.getDeckCount();
		Assert.assertEquals(954, decks3);
		
		
		mtgTop8Search.setCards("Devoted Druid");
		int decks4 = mtgTop8Search.getDeckCount();
		Assert.assertEquals(203, decks4);
	}
	
}
