package de.rohmio.mtg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.model.DeckboxCard;
import de.rohmio.mtg.model.DeckboxDeck;
import de.rohmio.scryfall.api.ScryfallApi;
import de.rohmio.scryfall.api.model.CardFaceObject;
import de.rohmio.scryfall.api.model.CardObject;
import de.rohmio.scryfall.api.model.ListObject;
import de.rohmio.scryfall.api.model.enums.Format;
import de.rohmio.scryfall.api.model.enums.Legality;
import retrofit2.Call;
import retrofit2.Response;

public class MtgStapleChecker {
	
	private static Logger log = Logger.getLogger(MtgStapleChecker.class.getName());

	private static final String ln = System.lineSeparator();
	
	private static final String deckBox = "https://deckbox.org/sets/%s";
	private static final String goldfish = "https://www.mtggoldfish.com/price/%s/%s";
	
	private static final String metagame = "https://www.mtggoldfish.com/metagame";
	
	private static ScryfallApi scryfallApi;

	public static void main(String[] args) throws IOException {
		initLogger();
		
		scryfallApi = new ScryfallApi();
		
		// get formats
//		List<String> formats = getMetagameFormats();
		List<String> formats = Arrays.asList(Format.values()).stream().map(f -> f.name()).collect(Collectors.toList());
		
		// get box
		List<DeckboxDeck> boxes = requestDeckIds("NudelForce", "Boxes");
		for(DeckboxDeck box : boxes) {
			doBox(box, formats);
		}
		
		log.info("closing scryfall connection");
		scryfallApi.close();
		log.info("end");
	}
	
	private static void initLogger() throws SecurityException, IOException {
		FileHandler handler = new FileHandler("latest.log");
		handler.setFormatter(new SimpleFormatter());
		log.addHandler(handler);
	}
	
	private static void doBox(DeckboxDeck box, List<String> formats) throws IOException {
		log.info("do box "+box);
		log.info("Requesting cards from deckbox.org deck");
		List<DeckboxCard> cards = parseDeckBox(box.getId());
		log.info("Received cards from deckbox.org deck: "+cards);

		// init storage file
//		File file = new File(boxId+".md");
//		initMdFile(file, formats);
		File file = new File("results/"+box.getName()+".csv");
		initCsvFile(file, formats);
		
		// go through cards in box
		List<Thread> threads = new ArrayList<>();
		for(DeckboxCard card : cards) {
			// no threading due to mtggoldfish throttle
//			Thread thread = new Thread(() -> {
				try {
					doCard(card, file, formats);
				} catch (IOException e) {
					e.printStackTrace();
				}
//			}, card.toString());
//			threads.add(thread);
//			thread.start();
		}
		
		// join all threads
		for(Thread thread : threads) {
			try {
				thread.join();
//				System.out.println(thread+" finished");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<DeckboxDeck> requestDeckIds(String userName, String directory) {
		log.info("Requesting decks from deckbox.org");
		Document doc = getDocument("https://deckbox.org/users/"+userName);
		Elements root = doc.select("span[data-title="+directory+"]");
		Element parent = root.parents().get(0);
		Element listing = parent.nextElementSibling();
		Elements select = listing.select("li[class=submenu_entry deck]");
		List<DeckboxDeck> deckIds = new ArrayList<>();
		for(Element s : select) {
			String id = s.attr("id");
			String deckId = id.replace("deck_", "");
			
			String deckName = s.select("a").attr("data-title");
			System.out.println(deckName);
			
			DeckboxDeck deckboxDeck = new DeckboxDeck(deckName, deckId);
			
			deckIds.add(deckboxDeck);
		}
		log.info("Received decks from deckbox.org: "+deckIds);
		return deckIds;
	}
	
	private static void initCsvFile(File file, List<String> formats) throws IOException {
		file.delete();
		FileUtils.writeStringToFile(file, "Name", "UTF-8", true);
		
		for(String format : formats) {
			FileUtils.writeStringToFile(file, ","+format, "UTF-8", true);
		}
		FileUtils.writeStringToFile(file, ln, "UTF-8", true);
	}
	
	private static void writeCsvLine(File file, String cardName, List<String> formats, Map<String, Integer> scores) throws IOException {
		String line = cardName.replace(",", "");
		for(String format : formats) {
			Integer score = scores.get(format);
			line += ",";
			if(score != null) {
				line += score;
			}
		}
		System.out.println(String.format("Writing in '%s': %s", file.getName(), line));
		FileUtils.writeStringToFile(file, line+ln, "UTF-8", true);
	}
	
	private static void initMdFile(File file, List<String> formats) throws IOException {
		log.info("init md file: "+file);
		log.info("deleting file: "+file);
		file.delete();
		FileUtils.writeStringToFile(file, "Name", "UTF-8", true);
		
		for(String format : formats) {
			FileUtils.writeStringToFile(file, " | "+format, "UTF-8", true);
		}
		FileUtils.writeStringToFile(file, ln, "UTF-8", true);
		
		String join = String.join("", Collections.nCopies(formats.size(), " | ---"));
		FileUtils.writeStringToFile(file, "---"+join+ln, "UTF-8", true);
	}
	
	private static void writeMdLine(File file, String cardName, List<String> formats, Map<String, Integer> scores) throws IOException {
		String line = cardName;
		for(String format : formats) {
			Integer score = scores.get(format);
			line += " |";
			if(score != null) {
				line += " "+score;
			}
		}
		System.out.println(String.format("Writing in '%s': %s", file.getName(), line));
		FileUtils.writeStringToFile(file, line+ln, "UTF-8", true);
	}
	
	private static void doCard(DeckboxCard card, File file, List<String> formats) throws IOException {
		System.out.println("Doing card: "+card);
		CardObject cardFromScryfall = getCardFromScryfall(card.getName());
		List<CardFaceObject> card_faces = cardFromScryfall.getCard_faces();
		String cardName;
		if(card_faces == null) {
			cardName = cardFromScryfall.getName();
		} else {
			String oracle_textFront = card_faces.get(0).getOracle_text();
			String type_lineBack = card_faces.get(1).getType_line();
			if(oracle_textFront.toLowerCase().contains("transform")
					|| type_lineBack.contains("Adventure")) {
				cardName = card_faces.get(0).getName();
			} else {
				cardName = cardFromScryfall.getName();
			}
		}
		String edition = cardFromScryfall.getSet_name();
		if(cardFromScryfall.getFoil()) {
			edition += ":Foil";
		}
		
		Map<String, Integer> scores = null;
		
		boolean goldfish = false;

		if(goldfish) {
			String regex = "[,':]|// ";
			cardName = cardName.replaceAll(regex, "");
			edition = SetMapping.scryfallToGoldfish(edition);
			List<CardStapleInfo> parseGoldfish = parseGoldfish(edition, cardName);
			if(parseGoldfish == null) {
				return;
			}
			scores = scoreMtgGoldfish(parseGoldfish);
		} else {
			scores = new HashMap<>();
			Map<Format, Legality> legalities = cardFromScryfall.getLegalities();
			for(Format format : legalities.keySet()) {
				Legality legality = legalities.get(format);
				if(legality == Legality.legal || legality == Legality.restricted) {
					String top8Code = format.getTop8Code();
					if(top8Code != null) {
						List<DeckInfo> deckInfos = MtgTop8Search.getDecksContainingCard(top8Code, true, true, 90, cardName, CompLevel.Professional, CompLevel.Major);
						int deckCount = deckInfos.size();
						scores.put(format.name(), deckCount);
					}
				}
			}
		}
		
//		System.out.println(String.format("Score of '%s': %s", cardName, scores));
		
//		writeMdLine(file, cardName, formats, scores);
		writeCsvLine(file, cardName, formats, scores);
	}
	
	private static Map<String, Integer> scoreMtgGoldfish(List<CardStapleInfo> cardStapleInfos) {
		Map<String, Integer> sums = new HashMap<>();
//		int differentDecks = cardStapleInfos.size();
//		int amountOfAllDecks = cardStapleInfos.stream().mapToInt(CardStapleInfo::getDeckCount).sum();
		for(CardStapleInfo cardStapleInfo : cardStapleInfos) {
			String format = cardStapleInfo.getFormat();
			int deckCount = cardStapleInfo.getDeckCount();
			Integer oldValue = sums.get(format);
			int newValue;
			if(oldValue == null) {
				newValue = deckCount;
			} else {
				newValue = oldValue + deckCount;
			}
			sums.put(format, newValue);
		}
		
		return sums;
	}
	
	private static List<String> getMetagameFormats() {
		log.info("Requesting metagame formats");
		Document doc = getDocument(metagame);
		Elements formatElements = doc.select(".deck-price-paper > .subNav-menu-desktop > a");
		List<String> formats = new ArrayList<>();
		for(Element formatElement : formatElements) {
			formats.add(formatElement.text());
		}
		log.info("Received metagame formats: "+formats);
		return formats;
	}

	private static List<CardStapleInfo> parseGoldfish(String setName, String cardName) throws IOException {
		String url = String.format(goldfish, setName, cardName);

		Document doc = getDocument(url);
		if(doc == null) {
			return null;
		}
		
		List<CardStapleInfo> cardStapleInfos = new ArrayList<>();
		
		Element recentDecksElement = doc.selectFirst(".price-card-recent-decks");
		if(doc.toString().contains("Throttled")) {
			try {
				System.err.println("got trottled. waiting 10 seconds.");
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return parseGoldfish(setName, cardName);
		}
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

	private static List<DeckboxCard> parseDeckBox(String deckId) throws IOException {
		String url = String.format(deckBox, deckId);

		Document doc = getDocument(url);
		Elements tableElements = doc.select("table[class*=set_cards]");
		Elements cardRows = tableElements.select("tr[id]");
		
		List<DeckboxCard> cards = new ArrayList<>();
		for(Element cardRow : cardRows) {
			DeckboxCard card = new DeckboxCard();
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
			// only add card if it is not yet in the list
			if(cards.stream().noneMatch(c -> card.getName().equals(c.getName()))) {
				cards.add(card);
			}
		}
		return cards;
	}
	
	private static Document getDocument(String url) {
		try {
//			System.out.println("Requesting: "+url);
			Connection connect = Jsoup.connect(url);
			Document document = connect.get();
			return document;
		} catch (IOException e) {
			String message = e.toString();
			if(message.contains("Status=404")) {
				System.err.println(e);
			} else if(message.contains("Status=502")) {
				// do nothing trying again
			} else {
				System.err.println(e);
			}
		}
		return null;
	}
	
	private static CardObject getCardFromScryfall(String cardName) {
		Response<ListObject<CardObject>> response = null;
		try {
			String query = String.format("is:firstprint !'%s'", cardName.replace("'", ""));
			Call<ListObject<CardObject>> call = scryfallApi.cards().cards(query);
//			HttpUrl url = call.request().url();
//			System.out.println("Requesting: "+url);
			response = call.execute();
			CardObject card = response.body().getData().get(0);
//			String name;
//			if(card.getCard_faces() == null) {
//				name = card.getName();
//			} else {
//				name = card.getCard_faces().get(0).getName();
//			}
//			System.out.println(String.format("%s (%s)", name, card.getSet_name()));
			return card;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
//			response.raw().body().close();
		}
		return null;
	}

}
