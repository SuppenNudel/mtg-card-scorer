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
import de.rohmio.mtg.scryfall.api.endpoints.SearchEndpoint;
import de.rohmio.mtg.scryfall.api.model.CardObject;
import de.rohmio.mtg.scryfall.api.model.Format;
import de.rohmio.mtg.scryfall.api.model.Legality;
import de.rohmio.mtg.scryfall.api.model.ListObject;
import de.rohmio.mtg.scryfall.api.model.Unique;

public class MtgCardScorer {

	private static Logger LOGGER = Logger.getLogger(MtgCardScorer.class.getName());

	public static MtgTop8Config CONFIG_MTGTOP8;
	public static DatabaseConfig CONFIG_DB;

	private static IOHandler ioHandler;

	public static void main(String[] args) throws IOException, InterruptedException {
		initLogger();
		initConfig(args);
		initScript();
		
		ExecutorService executor = Executors.newFixedThreadPool(20);

		String query = String.join(" or ", CONFIG_MTGTOP8.getFormats().stream().map(f -> "f:"+f).collect(Collectors.toList()));
		ListObject<CardObject> cardList = null;
		for (int page = 1; cardList == null || cardList.getHas_more(); ++page) {
			SearchEndpoint search = ScryfallApi.cards
					.search(query)
					.unique(Unique.cards)
					.includeExtras(false)
					.includeMultilingual(false)
					.includeVariations(false)
					.page(page);
			try {
				cardList = search.get();
			} catch (Exception e) {
				e.printStackTrace();
				page--;
				break;
			}
			if (cardList.getWarnings() != null) {
				LOGGER.warning(cardList.getWarnings().toString());
			}
			

			// filter out cards where their information is still relevant
			List<String> cardNamesNotNeededAnymore = ioHandler
					.getCardsNotNeeded(CONFIG_MTGTOP8.getRenewXdaysBefore())
					.stream()
					.map(CardStapleInfo::getCardname)
					.collect(Collectors.toList());
			List<CardObject> remainingCards = cardList
					.getData()
					.stream()
					.filter(c -> !cardNamesNotNeededAnymore.contains(c.getName()))
					.collect(Collectors.toList());
			LOGGER.info("Amount of cards to request in this cycle: " + remainingCards.size());
			
			// go through each card
			CountDownLatch latch = new CountDownLatch(remainingCards.size());
			
			for (CardObject scryfallCard : remainingCards) {
				executor.execute(() -> {
					try {
						System.out.println(scryfallCard);
						CardStapleInfo cardStapleInfo = gatherCardStapleInfo(scryfallCard);
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
		}
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
			if(args.length == 0) {
				CONFIG_DB = DatabaseConfig.loadConfig("@database.config");
				CONFIG_MTGTOP8 = MtgTop8Config.loadConfig("@mtgtop8.config");
			} else if(args.length == 2) {
				LOGGER.info("db_config: "+args[0]);
				CONFIG_DB = DatabaseConfig.loadConfig("@"+args[0]);

				LOGGER.info("mtgtop8_config: "+args[1]);
				CONFIG_MTGTOP8 = MtgTop8Config.loadConfig("@"+args[1]);
			}
			LOGGER.info("db_onfig: "+CONFIG_DB);
			LOGGER.info("mtgtop8_config: "+CONFIG_MTGTOP8);
		} catch (ParameterException e) {
			e.printStackTrace();
			e.getJCommander().usage();
			return;
		}
	}

	public static IOHandler initScript() throws IOException {
//		CsvHandler csvHandler = new CsvHandler(new File("results/competitive_score.csv"));
//		csvHandler.init(fields);
		SqlHandler sqlHandler = new SqlHandler(CONFIG_DB);
//		GoogleSheetsHandler googleSheetsHandler = new GoogleSheetsHandler();

		ioHandler = sqlHandler;
		ioHandler.init();
		LOGGER.info(String.format("Using %s as ioHandler", ioHandler));

		return ioHandler;
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


	public static CardStapleInfo gatherCardStapleInfo(CardObject scryfallCard) throws IOException {
		LOGGER.info("Doing card: " + scryfallCard);

		CardStapleInfo cardStapleInfo = new CardStapleInfo(scryfallCard.getName());

		List<Thread> threads = new ArrayList<>();
		for (Format format : CONFIG_MTGTOP8.getFormats()) {
			// is it even legal
			Legality legality = scryfallCard.getLegalities().get(format);
			if (Arrays.asList(Legality.banned, Legality.not_legal).contains(legality)) {
				cardStapleInfo.setFormatScore(format, -1);
			} else {
				for (CompLevel compLevel : CONFIG_MTGTOP8.getCompLevels()) {
					String cardName = scryfallCard.getName();
					Thread thread = new Thread(() -> {
						int deckCount = requestDeckCountMultiTry(cardName, format, compLevel, 3);
						int factor = CONFIG_MTGTOP8.getCompLevelFactor(compLevel);
						cardStapleInfo.setFormatScore(format, deckCount * factor);
					}, cardName + " in " + format);
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

    private static int requestDeckCountMultiTry(String normalizedCardName, Format format, CompLevel compLevel, int numberOfTries) {
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

    private static int requestDeckCount(String normalizedCardName, Format format, CompLevel compLevel) throws IOException {
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
    	.format(MtgTop8Format.valueOf(format.getTop8Code()))
    	.get();

        LOGGER.info(String.format("Requesting from mtgtop8; Card: '%s' Format: '%s'; CompLevel: '%s'",
                normalizedCardName, format, compLevel));
        return deckCount;
    }

}
