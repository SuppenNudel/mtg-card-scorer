package de.rohmio.mtg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class RequestTest {
	
	@Test
	public void compareScoringMethods() throws IOException {
		MtgTop8Search mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setStartDate("25/05/2019");
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setBoard(true, true);
		mtgTop8Search.setCards("Lightning Bolt");
		mtgTop8Search.setFormat(MtgTop8Format.MO);

		// all
//		System.out.println(calculateScore(mtgTop8Search.getDecks()));
		
		// up to prof
		mtgTop8Search.setCompLevel(CompLevel.Professional);
		System.out.println(calculateScore(mtgTop8Search.getDecks()));
		
		// up to major
		mtgTop8Search.setCompLevel(CompLevel.Major);
		System.out.println(calculateScore(mtgTop8Search.getDecks()));

		// up to competitive
		mtgTop8Search.setCompLevel(CompLevel.Competitive);
		System.out.println(calculateScore(mtgTop8Search.getDecks()));
		
		// gain all
		mtgTop8Search.setCompLevel(CompLevel.Regular);
		System.out.println(calculateScore(mtgTop8Search.getDecks()));
	}

	@Ignore
	@Test
	public void multipleRequestsForAllFormats() throws IOException {
		Date start = new Date();
		MtgTop8Search mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setStartDate("25/04/2020");
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setBoard(true, true);
		mtgTop8Search.setCards("Lightning Bolt");

		List<DeckInfo> allDecks = new ArrayList<>();
		for(MtgTop8Format format : MtgTop8Format.values()) {
			mtgTop8Search.setFormat(format);
			List<DeckInfo> decks = mtgTop8Search.getDecks();
			allDecks.addAll(decks);
		}
		System.out.println("Multiple deck size: "+allDecks.size());
		
		Date end = new Date();
		long time = end.getTime()-start.getTime();
		System.out.println("RequestTest.multipleRequestsForAllFormats() took "+time+" milliseconds");
	}
	
	@Ignore
	@Test
	public void singleRequestForAllFormats() throws IOException {
		Date start = new Date();
		MtgTop8Search mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setStartDate("25/04/2020");
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setBoard(true, true);
		mtgTop8Search.setCards("Lightning Bolt");

		List<DeckInfo> decks = mtgTop8Search.getDecks();
		System.out.println("Single deck size: "+decks.size());
		
		Date end = new Date();
		long time = end.getTime()-start.getTime();
		System.out.println("RequestTest.multipleRequestsForAllFormats() took "+time+" milliseconds");
	}
	
	@Ignore
	@Test
	public void requestDecks() throws IOException, InterruptedException {
		MtgTop8Search mtgTop8Search = new MtgTop8Search();
		
		mtgTop8Search.setStartDate("25/04/2020");
		mtgTop8Search.setEndDate("25/05/2020");
		mtgTop8Search.setFormat(MtgTop8Format.MO);
		mtgTop8Search.setBoard(true, true);

		mtgTop8Search.setCards("Lightning Bolt");
		
		List<DeckInfo> decks1 = mtgTop8Search.getDecks();
		System.out.println("Score of "+mtgTop8Search+" is "+calculateScore(decks1));
		Assert.assertEquals(87, decks1.size());

		
		mtgTop8Search.setCards("Birds of Paradise");

		List<DeckInfo> decks2 = mtgTop8Search.getDecks();
		System.out.println("Score of "+mtgTop8Search+" is "+calculateScore(decks2));
		Assert.assertEquals(18, decks2.size());
		
		
		mtgTop8Search.setCards("Leyline of the Void");
		
		List<DeckInfo> decks3 = mtgTop8Search.getDecks();
		System.out.println("Score of "+mtgTop8Search+" is "+calculateScore(decks3));
		Assert.assertEquals(0, decks3.size());
		
		
		mtgTop8Search.setCards("Devoted Druid");
		
		List<DeckInfo> decks4 = mtgTop8Search.getDecks();
		System.out.println("Score of "+mtgTop8Search+" is "+calculateScore(decks4));
		Assert.assertEquals(15, decks4.size());
	}
	
	private static int calculateScore(List<DeckInfo> decks) {
		int finalScore = 0;
		for(DeckInfo deckInfo : decks) {
			CompLevel level = deckInfo.getLevel();
			switch (level) {
			case Professional: finalScore += 5; break;
			case Major: finalScore += 3; break;
			case Competitive: finalScore += 2; break;
			case Regular: finalScore += 1; break;
			default:
				break;
			}
		}
		return finalScore;
	}

}
