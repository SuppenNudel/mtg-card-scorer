package de.rohmio.mtg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MtgStapleChecker {

	private static final String deckBox = "https://deckbox.org/sets/%s";
	private static final String goldfish = "https://www.mtggoldfish.com/price/%s/%s";

	public static void main(String[] args) throws IOException {
		List<Card> cards = parseDeckBox("01_White");
		System.out.println(cards);
		
		/*
		String[] cardNames = {
			"Lightning Bolt",
			"Devoted Druid",
			"Urzas Tower",
			"Slippery Bogle"
		};
		for(String cardName : cardNames) {
			List<CardStapleInfo> parseGoldfish = parseGoldfish("Masters 25", cardName);
			int score = score(parseGoldfish);
			System.out.println(String.format("Score of '%s': %s", cardName, score));
		}
		*/
	}
	
	private static int score(List<CardStapleInfo> cardStapleInfos) {
		int differentDecks = cardStapleInfos.size();
		int amountOfAllDecks = cardStapleInfos.stream().mapToInt(CardStapleInfo::getDeckCount).sum();
		System.out.println(differentDecks+"/"+amountOfAllDecks);
		return differentDecks*amountOfAllDecks;
	}

	private static List<CardStapleInfo> parseGoldfish(String setName, String cardName) throws IOException {
		String url = String.format(goldfish, setName, cardName);

//		Document doc = getDocument(url);
		Document doc = getDocument(cardName);
		
		List<CardStapleInfo> cardStapleInfos = new ArrayList<>();
		
		Element recentDecksElement = doc.selectFirst(".price-card-recent-decks");
		Elements rows = recentDecksElement.select("tr");
		for(Element row : rows) {
			Elements cells = row.select("td");
			CardStapleInfo cardStapleInfo = new CardStapleInfo();
			for(Element cell : cells) {
				String key = cell.attr("class");
				String value = cell.text();
				switch (key) {
				case "col-num-decks":
					// number of decks with this deck name
					cardStapleInfo.setDeckCount(Integer.parseInt(value));
					break;
				case "col-deck-name":
					// deck name
					cardStapleInfo.setDeckName(value);
					break;
				case "col-format":
					// format
					cardStapleInfo.setFormat(value);
					break;
				default:
					break;
				}
			}
			cardStapleInfos.add(cardStapleInfo);
		}
		return cardStapleInfos;
	}

	private static List<Card> parseDeckBox(String deckId) throws IOException {
		String url = String.format(deckBox, deckId);
		
		List<Card> cards = new ArrayList<>();

//		Document doc = getDocument(url);
		Document doc = getDocument(deckId);
		Elements tableElements = doc.select("table[class*=set_cards]");
		Elements cardRows = tableElements.select("tr[id]");
		for(Element cardRow : cardRows) {
			Card card = new Card();
			String cardName = cardRow.select("td[class=card_name] > a[class=simple]").text();
			card.setName(cardName);
			String editionFull = cardRow.select("div[class=mtg_edition_container] > img").attr("data-title");
			Pattern pattern = Pattern.compile("(.+?) \\(Card #(\\d+?)\\)");
			Matcher matcher = pattern.matcher(editionFull);
			boolean matches = matcher.matches();
			if(matches) {
				String edition = matcher.group(1);
				card.setEdition(edition);
				String collectorNumber = matcher.group(2);
				card.setCollectorNumber(Integer.parseInt(collectorNumber));
			} else {
				System.err.println(editionFull+" didn't match");
			}
			cards.add(card);
		}
		return cards;
	}
	
	private static Document getDocument(String url) {
		try {
//			System.out.println("Requesting: "+url);
			return Jsoup.parse(new File(url+".html"), "UTF-8");
//			return Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
