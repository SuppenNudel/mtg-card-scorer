package de.rohmio.mtg;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main {
	
	private static final String deckBox = "https://deckbox.org/sets/%s";
	private static final String goldfish = "https://www.mtggoldfish.com/price/%s/%s";

	public static void main(String[] args) throws IOException {
		parseGoldfish("Masters 25", "Lightning Bolt");
		
//		parseDeckBox("2615929");
		
	}
	
	private static void parseGoldfish(String setName, String cardName) throws IOException {
		String url = String.format(goldfish, setName, cardName);
		
		Document doc = Jsoup.connect(url).get();
		Elements recentDecksElement = doc.select(".price-card-recent-decks");
		System.out.println(recentDecksElement);
	}
	
	private static void parseDeckBox(String deckId) throws IOException {
		String url = String.format(deckBox, deckId);
		
		Document doc = Jsoup.connect(url).get();
	}

}
