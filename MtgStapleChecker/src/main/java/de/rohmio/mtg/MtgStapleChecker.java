package de.rohmio.mtg;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.write.GoogleSheetsHandler;
import de.rohmio.mtg.write.IOHandler;
import de.rohmio.scryfall.api.ScryfallApi;
import de.rohmio.scryfall.api.model.CardFaceObject;
import de.rohmio.scryfall.api.model.CardObject;
import de.rohmio.scryfall.api.model.ListObject;
import de.rohmio.scryfall.api.model.enums.CatalogType;
import de.rohmio.scryfall.api.model.enums.Format;
import de.rohmio.scryfall.api.model.enums.Layout;
import de.rohmio.scryfall.api.model.enums.Legality;

public class MtgStapleChecker {

	private static Logger log = Logger.getLogger(MtgStapleChecker.class.getName());

	private static Configuration config;

	public static final String FIELD_CARDNAME = "cardname";
	public static final String FIELD_TIMESTAMP = "timestamp";
	private static IOHandler ioHandler;

	private static ScryfallApi scryfallApi;
	public static List<String> formats;

	// mtgtop8 parameters
	private static boolean mainboard;
	private static boolean sideboard;
	private static int startXdaysbefore;
	private static int endXdaysbefore;
	private static int renewXdaysbefore;
	private static List<Legality> interrestingLegalities;
	private static List<Format> interrestingFormats;
	private static CompLevel[] compLevels;

	public static void main(String[] args) throws IOException, InterruptedException {
		initLogger();
		loadConfig();
		initScript();

		List<String> cardnames = scryfallApi.catalog().getCatalog(CatalogType.CARD_NAMES).execute().body().getData();
		log.info("Total amount of cards: " + cardnames.size());

		// filter out cards where their information is still relevant
		List<CardStapleInfo> cardsNotNeededAnymore = ioHandler.getCardsNotNeededAnymore(renewXdaysbefore);
		List<String> cardnamesNotNeededAnymore = cardsNotNeededAnymore.stream().map(c -> c.getCardname())
				.collect(Collectors.toList());
		List<String> remainingCards = cardnames.stream().filter(c -> !cardnamesNotNeededAnymore.contains(c))
				.collect(Collectors.toList());
		log.info("Amount of cards to request: " + remainingCards.size());

		ExecutorService executor = Executors.newFixedThreadPool(20);

		// go through each card
		CountDownLatch latch = new CountDownLatch(remainingCards.size());
		for (String cardname : remainingCards) {
			executor.execute(() -> {
				try {
					CardObject scryfallCard = getScryfallCard(cardname);
					System.out.println(scryfallCard);
					if(scryfallCard == null) {
						return;
					}
					if(scryfallCard.getLayout() == Layout.meld) {
						// do not analyze meld cards
						return;
					}
					CardStapleInfo cardStapleInfo = doCard(scryfallCard);
					if(cardStapleInfo != null) {
						ioHandler.addDataset(cardStapleInfo);
					}
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		latch.await();
		awaitTerminationAfterShutdown(executor);
		endScript();
	}
	
	public static void awaitTerminationAfterShutdown(ExecutorService threadPool) {
	    threadPool.shutdown();
	    try {
	        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
	            threadPool.shutdownNow();
	        }
	    } catch (InterruptedException ex) {
	        threadPool.shutdownNow();
	        Thread.currentThread().interrupt();
	    }
	}
	
	private static void endScript() {
		scryfallApi.close();
	}

	public static IOHandler initScript() throws IOException {
		formats = Arrays.asList(Format.values()).stream().filter(f -> interrestingFormats.contains(f))
				.filter(f -> f.getTop8Code() != null).map(f -> f.name()).collect(Collectors.toList());

		List<String> fields = new ArrayList<>(formats);
		fields.add(0, FIELD_CARDNAME);
		fields.add(1, FIELD_TIMESTAMP);

//		CsvHandler csvHandler = new CsvHandler(new File("results/competitive_score.csv"));
//		csvHandler.init(fields);
//		SqlHandler sqlHandler = new SqlHandler(config.getString("database.host"), config.getInt("database.port"),
//				config.getString("database.user"), config.getString("database.password", "secret"),
//				config.getString("database.database"), config.getString("database.table"));
		GoogleSheetsHandler googleSheetsHandler = new GoogleSheetsHandler();

		ioHandler = googleSheetsHandler;
		ioHandler.init(fields);
		log.info(String.format("Using %s as ioHandler", ioHandler));

		scryfallApi = new ScryfallApi();
		
		return ioHandler;
	}

	public static void loadConfig() {
		Configurations configs = new Configurations();
		try {
			config = configs.properties(new File("config.properties"));
			// access configuration properties

			mainboard = config.getBoolean("mtgtop8.mainboard");
			sideboard = config.getBoolean("mtgtop8.sideboard");
			startXdaysbefore = config.getInt("mtgtop8.startXdaysbefore");
			endXdaysbefore = config.getInt("mtgtop8.endXdaysbefore");
			renewXdaysbefore = config.getInt("mtgtop8.renewXdaysbefore");

			String[] formatsStringArray = config.getString("mtgtop8.formats").split(",");
			interrestingFormats = new ArrayList<>();
			for (String formatString : formatsStringArray) {
				interrestingFormats.add(Format.valueOf(formatString));
			}

			String[] legalityStringArray = config.getString("mtgtop8.legalities").split(",");
			interrestingLegalities = new ArrayList<>();
			for (String string : legalityStringArray) {
				interrestingLegalities.add(Legality.valueOf(string));
			}

			String[] complevelStringArray = config.getString("mtgtop8.complevels").split(",");
			List<CompLevel> compLevelsList = new ArrayList<>();
			for (String string : complevelStringArray) {
				compLevelsList.add(CompLevel.valueOf(string));
			}
			compLevels = compLevelsList.toArray(new CompLevel[compLevelsList.size()]);

		} catch (ConfigurationException e) {
			// Something went wrong
			e.printStackTrace();
		}
	}

	/*
	 * private static void endScript() { log.info("closing scryfall connection");
	 * scryfallApi.close(); log.info("end"); }
	 */

	private static void initLogger() throws SecurityException, IOException {
		new File("logs").mkdir();
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String timestamp = format.format(new Date());
		FileHandler handler = new FileHandler("logs/" + timestamp + ".log");
		handler.setFormatter(new SimpleFormatter());
		log.addHandler(handler);
	}

	private static String normalizeCardName(CardObject scryfallCard) {
		String normalizedCardname = scryfallCard.getName();
		List<CardFaceObject> card_faces = scryfallCard.getCard_faces();
		Layout layout = scryfallCard.getLayout();
		switch (layout) {
		case normal:
		case planar:
		case host:
		case vanguard:
		case split:
		case scheme:
			break;
		case meld:
			throw new RuntimeException("Meld cards should not get analyzed");
		case modal_dfc:
		case transform:
		case flip:
		case adventure:
			normalizedCardname = card_faces.get(0).getName();
			break;
		default:
			break;
		}
		return normalizedCardname;
	}

	public static CardObject getScryfallCard(String cardname) throws IOException {
		String q = String.format("!\"%s\" lang:en -set_type:memorabilia -set_type:funny -layout:scheme -layout:planar -layout:vanguard -type:token", cardname);
		ListObject<CardObject> foundCards = scryfallApi.cards()
				.search(q, null, null, null, true, null, null, null, null, null).execute().body();
		if (foundCards == null) {
			log.severe(String.format("Card not found '%s'. Might be a 'funny' card.", cardname));
			return null;
		}
		if(foundCards.getTotal_cards() != 1) {
			log.severe(String.format("Cardname '%s' is ambiguous.", cardname));
			return null;
		}
		CardObject scryfallCard = foundCards.getData().get(0);
		return scryfallCard;
	}

	public static CardStapleInfo doCard(CardObject scryfallCard) throws IOException {
		log.info("Doing card: " + scryfallCard);

		CardStapleInfo cardStapleInfo = new CardStapleInfo(scryfallCard.getName());

		List<Thread> threads = new ArrayList<>();
		Map<Format, Legality> cardLegalities = scryfallCard.getLegalities();
		// go through formats in which the card is legal

		for (Format format : interrestingFormats) {
			String top8FormatCode = format.getTop8Code();
			// if this format can not be looked up on mtgtop8 skip it
			if (top8FormatCode == null) {
				continue;
			}

			Legality legality = cardLegalities.get(format);
			// if card is not legal in this format skip it
			if (!interrestingLegalities.contains(legality)) {
				cardStapleInfo.setFormatScore(format, -1);
				continue;
			}

			
			String normalizedCardName = normalizeCardName(scryfallCard);

			Thread thread = new Thread(() -> {
				MtgTop8Search mtgTop8Search = new MtgTop8Search();
				mtgTop8Search.setBoard(mainboard, sideboard);
				mtgTop8Search.setStartDate(startXdaysbefore);
				mtgTop8Search.setEndDate(endXdaysbefore);
				mtgTop8Search.setCards(normalizedCardName);
				mtgTop8Search.setCompLevel(compLevels);

				mtgTop8Search.setFormat(MtgTop8Format.valueOf(top8FormatCode));

				// repeat request for as long as it fails
				boolean successful = false;
				for (int tryNo = 0; tryNo <= 3 && !successful; ++tryNo) {
					for (CompLevel compLevel : compLevels) {
						try {
							log.info(String.format("Requesting from mtgtop8; Card: '%s' Format: '%s'; CompLevel: '%s'",
									scryfallCard, format, compLevel));
							int deckCount = mtgTop8Search.getDeckCount();
							cardStapleInfo.setFormatScore(format, deckCount * compLevel.getFactor());
							successful = true;
						} catch (IOException e) {
							// repeat
							log.warning(String.format("%s on %s; repeating action tryNo %s", e.getMessage(),
									Arrays.asList(mtgTop8Search.getCards()), tryNo));
						}
					}
				}
				if (!successful) {
					log.warning(String.format("repeated request often enough, still no response on card: %s",
							Arrays.asList(mtgTop8Search.getCards())));
				}
			}, normalizedCardName + " in " + format);
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

		log.info("Collected all infos about card: " + scryfallCard);

		return cardStapleInfo;
	}

}
