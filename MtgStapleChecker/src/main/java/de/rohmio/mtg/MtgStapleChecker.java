package de.rohmio.mtg;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.model.DeckboxCard;
import de.rohmio.mtg.model.DeckboxDeck;
import de.rohmio.mtg.write.IOHandler;
import de.rohmio.mtg.write.SqlHandler;
import de.rohmio.scryfall.api.ScryfallApi;
import de.rohmio.scryfall.api.model.CardFaceObject;
import de.rohmio.scryfall.api.model.CardObject;
import de.rohmio.scryfall.api.model.enums.Format;
import de.rohmio.scryfall.api.model.enums.Legality;

public class MtgStapleChecker {
	
	private static Logger log = Logger.getLogger(MtgStapleChecker.class.getName());

	private static final String urlDeckboxDeck = "https://deckbox.org/sets/%s";
	private static final String urlDeckboxUser = "https://deckbox.org/users/%s";

	private static final String sqlHost = "62.108.32.180";
	private static final String sqlPort = "3306";
	private static final String sqlUser = "zfdzt_root";
	private static final String sqlPassword = "dnfo*Ec79Hy3jhhA";
	private static final String sqlDatabase = "mtg-collection";
	private static final String sqlTable = "competitive_score";
	
	private static Legality[] interrestingLegalities = { Legality.legal, Legality.restricted };
	
	public static final String FIELD_CARDNAME = "cardname";
	public static final String FIELD_TIMESTAMP = "timestamp";
	private static IOHandler ioHandler;
	
	private static ScryfallApi scryfallApi;
	public static List<String> formats;
	
	// parameters
	private static int lastXdays = 4*30; // last 4 months
	private static String deckboxUser = "NudelForce";
	private static String deckboxDirecory = "Boxes";
	private static boolean mainboard = true;
	private static boolean sideboard = true;
//	private static CompLevel[] compLevels = { CompLevel.Professional, CompLevel.Major };

	public static void main(String[] args) throws IOException {
		initLogger();
		initScript();

		// get boxes
		List<DeckboxDeck> boxes = requestDeckIds(deckboxUser, deckboxDirecory);
		Set<String> cardnames = new HashSet<>();
		for(DeckboxDeck box : boxes) {
			List<DeckboxCard> cards = parseDeckBox(box.getId());
			cards.forEach(c -> cardnames.add(c.getName()));
		}
		
		// filter out cards where their information is still relevant
		List<CardStapleInfo> cardsNotNeededAnymore = ioHandler.getCardsNotNeededAnymore(lastXdays / 2);
		List<String> mapped = cardsNotNeededAnymore.stream().map(c -> c.getCardname()).collect(Collectors.toList());
		List<String> remainingCards = cardnames.stream().filter(cn -> !mapped.contains(cn)).collect(Collectors.toList());
		
		// go through cards in box
		for(String cardname : remainingCards) {
			try {
				doCard(cardname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		endScript();
	}
	
	private static void initScript() throws IOException {
		formats = Arrays.asList(Format.values()).stream()
				.filter(f -> f.getTop8Code() != null)
				.map(f -> f.name()).collect(Collectors.toList());

		List<String> fields = new ArrayList<>(formats);
		fields.add(0, FIELD_CARDNAME);
		fields.add(1, FIELD_TIMESTAMP);
		
//		CsvHandler csvHandler = new CsvHandler(new File("results/competitive_score.csv"));
//		csvHandler.init(fields);
		SqlHandler sqlHandler = new SqlHandler(sqlHost, sqlPort, sqlUser, sqlPassword, sqlDatabase, sqlTable);
		sqlHandler.init(fields);

		ioHandler = sqlHandler;
		log.info(String.format("Using %s as ioHandler", ioHandler));
		
		scryfallApi = new ScryfallApi();
	}
	
	private static void endScript() {
		log.info("closing scryfall connection");
		scryfallApi.close();
		log.info("end");
	}
	
	private static void initLogger() throws SecurityException, IOException {
		new File("logs").mkdir();
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String timestamp = format.format(new Date());
		FileHandler handler = new FileHandler("logs/"+timestamp+".log");
		handler.setFormatter(new SimpleFormatter());
		log.addHandler(handler);
	}
	
	private static List<DeckboxDeck> requestDeckIds(String userName, String directory) {
		
		log.info("Requesting decks from deckbox.org");
		Document doc = getDocument(String.format(urlDeckboxUser, userName));
		Elements root = doc.select("span[data-title="+directory+"]");
		Element parent = root.parents().get(0);
		Element listing = parent.nextElementSibling();
		Elements select = listing.select("li[class=submenu_entry deck]");
		List<DeckboxDeck> deckboxDecks = new ArrayList<>();
		for(Element s : select) {
			String id = s.attr("id");
			String deckId = id.replace("deck_", "");
			String deckName = s.select("a").attr("data-title");
			
			DeckboxDeck deckboxDeck = new DeckboxDeck(deckName, deckId);
			deckboxDecks.add(deckboxDeck);
		}
		log.info("Received decks from deckbox.org: "+deckboxDecks);
		return deckboxDecks;
	}
	
	private static String normalizeCardName(CardObject scryfallCard) {
		String normalizedCardname = scryfallCard.getName();
		List<CardFaceObject> card_faces = scryfallCard.getCard_faces();
		if(card_faces != null) {
			// TODO add fields for special cases
			if(scryfallCard.getType_line().contains("Adventure")) {
				normalizedCardname = card_faces.get(0).getName();
			}
		}
		return normalizedCardname;
	}
	
	private static void doCard(String cardname) throws IOException {
		CardObject scryfallCard = scryfallApi.cards().cardByName(cardname, null).execute().body();
		log.info("Doing card: "+cardname);
		
		final Map<String, String> values = new HashMap<>();
		values.put(FIELD_CARDNAME, cardname	);
		
		List<Thread> threads = new ArrayList<>();
		Map<Format, Legality> cardLegalities = scryfallCard.getLegalities();
		// go through formats in which the card is legal
		for(Format format : Format.values()) {
			String top8FormatCode = format.getTop8Code();
			// if this format can not be looked up on mtgtop8 skip it
			if(top8FormatCode == null) {
				continue;
			}
			
			Legality legality = cardLegalities.get(format);
			// if card is not legal in this format skip it
			if(!Arrays.asList(interrestingLegalities).contains(legality)) {
				values.put(format.name(), "-1");
				continue;
			}
			Thread thread = new Thread(() -> {
				MtgTop8Search mtgTop8Search = new MtgTop8Search();
				mtgTop8Search.setBoard(mainboard, sideboard);
				mtgTop8Search.setStartDate(lastXdays);
				mtgTop8Search.setCards(normalizeCardName(scryfallCard));
//				mtgTop8Search.setCompLevel(compLevels); // request every comp level
				
				mtgTop8Search.setFormat(MtgTop8Format.valueOf(top8FormatCode));
				
				// repeat request for as long as it fails
				List<DeckInfo> decks = null;
				for(int tryNo=0; tryNo<=10 && decks == null; ++tryNo) {
					try {
						
						decks = mtgTop8Search.getDecks();
						int score = calculateScore(decks);
						values.put(format.name(), String.valueOf(score));
					} catch (IOException e) {
						// repeat
						log.warning(String.format("%s on %s; repeating action tryNo %s", e.getMessage(), Arrays.asList(mtgTop8Search.getCards()), tryNo));
					}
				}
				if(decks == null) {
					log.warning(String.format("repeated request often enough, still no response on card: %s", Arrays.asList(mtgTop8Search.getCards())));
				}
			});
			threads.add(thread);
			thread.start();
		}
		threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		log.info("Collected all infos about card: "+cardname);

		ioHandler.addDataset(values);
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
	
	private static List<DeckboxCard> parseDeckBox(String deckId) throws IOException {
		log.info("Requesting cards from deckbox.org deck");
		String url = String.format(urlDeckboxDeck, deckId);

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
				log.warning(String.format("Edition '%s' did not match the format", editionFull));
			}
			// only add card if it is not yet in the list
			if(cards.stream().noneMatch(c -> card.getName().equals(c.getName()))) {
				cards.add(card);
			}
		}
		log.info("Received cards from deckbox.org deck: "+cards);
		return cards;
	}
	
	private static Document getDocument(String url) {
		try {
			Connection connect = Jsoup.connect(url);
			Document document = connect.get();
			return document;
		} catch (IOException e) {
			String message = e.toString();
			if(message.contains("Status=404")) {
				log.warning(e.toString());
			} else if(message.contains("Status=502")) {
				log.warning(e.toString()+"; trying again");
				return getDocument(url);
			} else {
				log.warning(e.toString());
			}
		}
		return null;
	}
	
}
