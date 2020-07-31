package de.rohmio.mtg;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequestTest {
	
	private MtgTop8Search mtgTop8Search;
	
	@Before
	public void generateBase() {
		mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setBoard(true, true);
		mtgTop8Search.setFormat(MtgTop8Format.ST);
	}
	
	@Test
	public void normalTest() throws IOException {
		mtgTop8Search.setCards("Abrade");
		int deckCount = mtgTop8Search.getDeckCount();
		Assert.assertEquals(2122, deckCount);
	}
	
	@Test
	public void apostropheTest() throws IOException {
		mtgTop8Search.setCards("Admiral's Order");
		int deckCount = mtgTop8Search.getDeckCount();
		Assert.assertEquals(18, deckCount);
	}
	
	@Test
	public void umlautTest() throws IOException {
		mtgTop8Search.setCards("Jötun Grunt");
		int deckCount = mtgTop8Search.getDeckCount();
		Assert.assertEquals(2, deckCount);
	}
	
	@Test
	public void aeTest() throws IOException {
		mtgTop8Search.setCards("Æther Adept");
		int deckCount = mtgTop8Search.getDeckCount();
		Assert.assertEquals(22, deckCount);
	}

}
