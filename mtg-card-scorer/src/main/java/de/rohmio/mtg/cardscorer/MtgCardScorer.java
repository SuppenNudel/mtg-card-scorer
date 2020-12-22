package de.rohmio.mtg.cardscorer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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

import de.rohm.io.mtg.mtgtop8.api.model.CompLevel;
import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;
import de.rohmio.mtg.cardscore.database.CardStapleInfo;
import de.rohmio.mtg.cardscore.database.DatabaseConfig;
import de.rohmio.mtg.cardscore.database.SqlConnector;
import de.rohmio.mtg.cardscore.database.StorageConnector;
import de.rohmio.mtg.cardscorer.config.MtgTop8Config;
import de.rohmio.mtg.mtgtop8.api.MtgTop8Api;
import de.rohmio.mtg.scryfall.api.ScryfallApi;
import de.rohmio.mtg.scryfall.api.endpoints.SearchEndpoint;
import de.rohmio.mtg.scryfall.api.model.CardObject;
import de.rohmio.mtg.scryfall.api.model.Direction;
import de.rohmio.mtg.scryfall.api.model.Format;
import de.rohmio.mtg.scryfall.api.model.Layout;
import de.rohmio.mtg.scryfall.api.model.Legality;
import de.rohmio.mtg.scryfall.api.model.Sorting;
import de.rohmio.mtg.scryfall.api.model.Unique;

public class MtgCardScorer {

	private static Logger LOGGER = Logger.getLogger(MtgCardScorer.class.getName());

	public static MtgTop8Config CONFIG_MTGTOP8;
	public static DatabaseConfig CONFIG_DB;

	private static StorageConnector storageConnector;

	public static void main(String[] args) throws IOException, InterruptedException {
		initLogger();
		initConfig(args);
		initScript();

		ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(args[0]));

		String query = String.join(" or ", CONFIG_MTGTOP8.getFormats().stream().map(f -> "f:"+f).collect(Collectors.toList()));
		query = "("+query+")";
		List<String> configCardNames = CONFIG_MTGTOP8.getCardNames();
		if(configCardNames != null) {
			query += " ("+String.join(" or ", configCardNames)+")";
		}
		query += " AND not:reprint";

		System.out.println(query);
		
		SearchEndpoint pagedSearch = ScryfallApi.cards
				.search(query)
				.order(Sorting.RELEASED)
				.dir(Direction.DESC)
				.unique(Unique.CARDS)
				.includeExtras(false)
				.includeMultilingual(false)
				.includeVariations(false);
		
		pagedSearch.getAll(cardList -> {
			// filter out cards where their information is still relevant
			List<String> cardNamesNotNeededAnymore = storageConnector
					.getCardsNotNeeded(CONFIG_MTGTOP8.getRenewXdaysBefore(), CONFIG_MTGTOP8.getFormats())
					.stream()
					.map(CardStapleInfo::getCardName)
					.collect(Collectors.toList());
			List<CardObject> remainingCards = cardList
					.stream()
					.filter(c -> !cardNamesNotNeededAnymore.contains(c.getName()))
					.collect(Collectors.toList());

			LOGGER.info("Amount of cards to request in this cycle: " + remainingCards.size());

			CountDownLatch latch = new CountDownLatch(remainingCards.size());
			for (CardObject scryfallCard : remainingCards) {
				executor.execute(() -> {
					try {
						System.out.println(scryfallCard);
						CardStapleInfo cardStapleInfo = gatherCardStapleInfo(scryfallCard);
						if(cardStapleInfo == null) {
							LOGGER.severe("No information about "+scryfallCard+" from mtgtop8");
						} else {
							storageConnector.addDataset(cardStapleInfo);
						}
						latch.countDown();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		});
		awaitTerminationAfterShutdown(executor);
	}

	private static void initLogger() throws SecurityException, IOException {
		new File("logs").mkdir();
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String timestamp = format.format(new Date());
		FileHandler handler = new FileHandler("logs/" + timestamp + ".log");
		handler.setFormatter(new SimpleFormatter());
		LOGGER.addHandler(handler);
	}

	public static void initConfig(String[] args) {
		try {
			if(args.length == 1) {
				CONFIG_DB = DatabaseConfig.loadConfig("@database.config");
				CONFIG_MTGTOP8 = MtgTop8Config.loadConfig("@mtgtop8.config");
			} else if(args.length == 3) {
				LOGGER.info("db_config: "+args[1]);
				CONFIG_DB = DatabaseConfig.loadConfig("@"+args[1]);

				LOGGER.info("mtgtop8_config: "+args[2]);
				CONFIG_MTGTOP8 = MtgTop8Config.loadConfig("@"+args[2]);
			}
			LOGGER.info("db_onfig: "+CONFIG_DB);
			LOGGER.info("mtgtop8_config: "+CONFIG_MTGTOP8);
		} catch (ParameterException e) {
			e.printStackTrace();
			e.getJCommander().usage();
			return;
		}
	}

	public static StorageConnector initScript() throws IOException {
		StorageConnector storageConnector = new SqlConnector(CONFIG_DB);

		MtgCardScorer.storageConnector = storageConnector;
		LOGGER.info(String.format("Using %s as ioHandler", storageConnector));

		return storageConnector;
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

	private static String toMtgTop8CardName(CardObject scryfallCard) {
		if(scryfallCard.getCard_faces() == null || scryfallCard.getLayout() == Layout.SPLIT) {
			return scryfallCard.getName();
		} else {
			return scryfallCard.getCard_faces().get(0).getName();
		}
	}

	public static CardStapleInfo gatherCardStapleInfo(CardObject scryfallCard) throws IOException {
		LOGGER.info("Doing card: " + scryfallCard);

		CardStapleInfo cardStapleInfo = new CardStapleInfo(scryfallCard.getName());
		cardStapleInfo.setTimestamp(LocalDateTime.now());

		String mtgtop8CardName = toMtgTop8CardName(scryfallCard);

		List<Thread> threads = new ArrayList<>();
		for (MtgTop8Format format : CONFIG_MTGTOP8.getFormats()) {
			// is it even legal
			Format scryfallFormat = Format.valueOf(format.name());
			Legality legality = scryfallCard.getLegalities().get(scryfallFormat);
			if (Arrays.asList(Legality.BANNED, Legality.NOT_LEGAL).contains(legality)) {
				cardStapleInfo.setFormatScore(format, -1);
			} else {
				for (CompLevel compLevel : CONFIG_MTGTOP8.getCompLevels()) {
					Thread thread = new Thread(() -> {
						int deckCount = requestDeckCountMultiTry(mtgtop8CardName, format, compLevel, 3);
						int factor = CONFIG_MTGTOP8.getCompLevelFactor(compLevel);
						cardStapleInfo.setFormatScore(format, deckCount * factor);
					}, mtgtop8CardName + " in " + format);
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

		LOGGER.info("Collected all infos about card: " + scryfallCard);
		return cardStapleInfo;
	}

    private static int requestDeckCountMultiTry(String normalizedCardName, MtgTop8Format format, CompLevel compLevel, int numberOfTries) {
        boolean successful = false;
        for (int tryNo = 0; tryNo <= 3 && !successful; ++tryNo) {
            try {
                int deckCount = requestDeckCount(normalizedCardName, format, compLevel);
                successful = true;
                return deckCount;
            } catch (IOException e) {
                LOGGER.warning(String.format("%s on %s; repeating action tryNo %s", e.getMessage(),
                        normalizedCardName, tryNo));
            }
        }
        if (!successful) {
            LOGGER.warning(String.format("repeated request often enough, still no response on card: %s",
                    normalizedCardName));
        }
        return -1;
    }

    private static int requestDeckCount(String normalizedCardName, MtgTop8Format format, CompLevel compLevel) throws IOException {
    	int startXdaysbefore = CONFIG_MTGTOP8.getStartXdaysBefore();
    	Calendar startdate = Calendar.getInstance();
		startdate.add(Calendar.DAY_OF_YEAR, -startXdaysbefore);

		int endXdaysbefore = CONFIG_MTGTOP8.getEndXdaysBefore();
    	Calendar enddate = Calendar.getInstance();
    	enddate.add(Calendar.DAY_OF_YEAR, -endXdaysbefore);

    	Integer deckCount = MtgTop8Api.sarch()
    	.mainboard(CONFIG_MTGTOP8.isMainboard())
    	.sideboard(CONFIG_MTGTOP8.isSideboard())
    	.startdate(startdate)
    	.enddate(enddate)
    	.cards(normalizedCardName)
    	.compLevel(compLevel)
    	.format(MtgTop8Format.valueOf(format.getId()))
    	.get();

        LOGGER.info(String.format("Requesting from mtgtop8; Card: '%s' Format: '%s'; CompLevel: '%s'",
                normalizedCardName, format, compLevel));
        return deckCount;
    }

}
