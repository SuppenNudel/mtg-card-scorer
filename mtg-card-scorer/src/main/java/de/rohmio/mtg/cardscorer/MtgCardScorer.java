package de.rohmio.mtg.cardscorer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import com.beust.jcommander.ParameterException;

import de.rohmio.mtg.cardscorer.config.DatabaseConfig;
import de.rohmio.mtg.cardscorer.config.MtgTop8Config;
import de.rohmio.mtg.cardscorer.io.IOHandler;
import de.rohmio.mtg.cardscorer.io.SqlHandler;
import de.rohmio.mtg.cardscorer.model.CardStapleInfo;
import de.rohmio.mtg.mtgtop8.api.MtgTop8Api;
import de.rohmio.mtg.mtgtop8.api.model.CompLevel;
import de.rohmio.mtg.mtgtop8.api.model.MtgTop8Format;
import de.rohmio.mtg.scryfall.api.ScryfallApi;
import de.rohmio.mtg.scryfall.api.model.CardFaceObject;
import de.rohmio.mtg.scryfall.api.model.CardObject;
import de.rohmio.mtg.scryfall.api.model.CatalogObject;
import de.rohmio.mtg.scryfall.api.model.CatalogType;
import de.rohmio.mtg.scryfall.api.model.Format;
import de.rohmio.mtg.scryfall.api.model.Layout;
import de.rohmio.mtg.scryfall.api.model.Legality;
import de.rohmio.mtg.scryfall.api.model.ListObject;

public class MtgCardScorer {

	private static Logger log = Logger.getLogger(MtgCardScorer.class.getName());

	private static MtgTop8Config mtgtop8_config;
	private static DatabaseConfig db_config;

	private static IOHandler ioHandler;

	public static List<String> formats;
	public static void main(String[] args) throws IOException, InterruptedException {
		initLogger();
		try {
			if(args.length == 0) {
				db_config = DatabaseConfig.loadConfig("@database.config");
				mtgtop8_config = MtgTop8Config.loadConfig("@mtgtop8.config");
			} else if(args.length == 2) {
				log.info("db_config: "+args[0]);
				db_config = DatabaseConfig.loadConfig("@"+args[0]);

				log.info("mtgtop8_config: "+args[1]);
				mtgtop8_config = MtgTop8Config.loadConfig("@"+args[1]);
			}
			log.info("db_onfig: "+db_config);
			log.info("mtgtop8_config: "+mtgtop8_config);
		} catch (ParameterException e) {
			e.printStackTrace();
			e.getJCommander().usage();
			return;
		}
		initScript();

		CatalogObject cardNamesCatalog = ScryfallApi.catalogs.catalog(CatalogType.CARD_NAMES).get();
		List<String> cardnames = cardNamesCatalog.getData();
		log.info("Total amount of cards: " + cardnames.size());

		// filter out cards where their information is still relevant
		List<CardStapleInfo> cardsNotNeededAnymore = ioHandler.getCardsNotNeeded(mtgtop8_config.getRenewXdaysBefore());
		List<String> cardnamesNotNeededAnymore = cardsNotNeededAnymore.stream().map(CardStapleInfo::getCardname)
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
					if(shouldDoCard(scryfallCard)) {
						CardStapleInfo cardStapleInfo = gatherCardStapleInfo(scryfallCard);
						if(cardStapleInfo != null) {
							ioHandler.addDataset(cardStapleInfo);
						}
					}
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		latch.await();
		awaitTerminationAfterShutdown(executor);
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

	private static boolean shouldDoCard(CardObject scryfallCard) {
		if(scryfallCard == null) {
			return false;
		}
		if(scryfallCard.getLayout() == Layout.meld) {
			// do not analyze meld cards
			return false;
		}
		return true;
	}

	public static IOHandler initScript() throws IOException {
		formats = Arrays.asList(Format.values()).stream().filter(f -> mtgtop8_config.getFormats().contains(f))
				.filter(f -> f.getTop8Code() != null).map(Format::name).collect(Collectors.toList());


//		CsvHandler csvHandler = new CsvHandler(new File("results/competitive_score.csv"));
//		csvHandler.init(fields);
		SqlHandler sqlHandler = new SqlHandler(db_config);
//		GoogleSheetsHandler googleSheetsHandler = new GoogleSheetsHandler();

		ioHandler = sqlHandler;
		ioHandler.init();
		log.info(String.format("Using %s as ioHandler", ioHandler));

		return ioHandler;
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

	// TODO do not request card if it is not legal anyway

	public static CardObject getScryfallCard(String cardname) throws IOException {
		String query = String.format("!\"%s\" lang:en -set_type:memorabilia -set_type:funny -layout:scheme -layout:planar -layout:vanguard -type:token", cardname);
		ListObject<CardObject> foundCards = ScryfallApi.cards.search(query).get();
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

	public static CardStapleInfo gatherCardStapleInfo(CardObject scryfallCard) throws IOException {
		log.info("Doing card: " + scryfallCard);

		CardStapleInfo cardStapleInfo = new CardStapleInfo(scryfallCard.getName());

		List<Thread> threads = new ArrayList<>();
		for (Format format : mtgtop8_config.getFormats()) {
			// is it even legal
			Legality legality = scryfallCard.getLegalities().get(format);
			if (legality == Legality.banned || legality == Legality.not_legal) {
				cardStapleInfo.setFormatScore(format, -1);
			} else {
				for (CompLevel compLevel : mtgtop8_config.getCompLevels()) {
					String normalizedCardName = normalizeCardName(scryfallCard);
					Thread thread = new Thread(() -> {
						int deckCount = requestDeckCountMultiTry(normalizedCardName, format, compLevel, 3);
						cardStapleInfo.setFormatScore(format, deckCount * compLevel.getFactor());
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
			}
		}

		log.info("Collected all infos about card: " + scryfallCard);
		return cardStapleInfo;
	}

    private static int requestDeckCountMultiTry(String normalizedCardName, Format format, CompLevel compLevel, int numberOfTries) {
        boolean successful = false;
        for (int tryNo = 0; tryNo <= 3 && !successful; ++tryNo) {
            try {
                int deckCount = requestDeckCount(normalizedCardName, format, compLevel);
                successful = true;
                return deckCount;
            } catch (IOException e) {
                log.warning(String.format("%s on %s; repeating action tryNo %s", e.getMessage(),
                        normalizedCardName, tryNo));
            }
        }
        if (!successful) {
            log.warning(String.format("repeated request often enough, still no response on card: %s",
                    normalizedCardName));
        }
        return -1;
    }

    private static int requestDeckCount(String normalizedCardName, Format format, CompLevel compLevel) throws IOException {
    	int startXdaysbefore = mtgtop8_config.getStartXdaysBefore();
    	Calendar startdate = Calendar.getInstance();
		startdate.add(Calendar.DAY_OF_YEAR, -startXdaysbefore);

		int endXdaysbefore = mtgtop8_config.getEndXdaysBefore();
    	Calendar enddate = Calendar.getInstance();
    	enddate.add(Calendar.DAY_OF_YEAR, -endXdaysbefore);

    	Integer deckCount = MtgTop8Api.sarch()
    	.mainboard(mtgtop8_config.isMainboard())
    	.sideboard(mtgtop8_config.isSideboard())
    	.startdate(startdate)
    	.enddate(enddate)
    	.cards(normalizedCardName)
    	.compLevel(compLevel)
    	.format(MtgTop8Format.valueOf(format.getTop8Code()))
    	.get();

        log.info(String.format("Requesting from mtgtop8; Card: '%s' Format: '%s'; CompLevel: '%s'",
                normalizedCardName, format, compLevel));
        return deckCount;
    }

}