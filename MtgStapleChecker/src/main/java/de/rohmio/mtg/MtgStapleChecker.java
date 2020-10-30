package de.rohmio.mtg;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.mtg.write.IOHandler;
import de.rohmio.mtg.write.SqlHandler;
import de.rohmio.scryfall.api.ScryfallApi;
import de.rohmio.scryfall.api.model.CardFaceObject;
import de.rohmio.scryfall.api.model.CardObject;
import de.rohmio.scryfall.api.model.ListObject;
import de.rohmio.scryfall.api.model.enums.Format;
import de.rohmio.scryfall.api.model.enums.Layout;

public class MtgStapleChecker {

	private static Logger log = Logger.getLogger(MtgStapleChecker.class.getName());

	private static Config config;
	
	private static IOHandler ioHandler;

	public static List<String> formats;
	public static void main(String[] args) throws IOException, InterruptedException {
		initLogger();
		config = Config.loadConfig();
		initScript();

		List<String> cardnames = ScryfallApi.cardNames().get().getData();
		log.info("Total amount of cards: " + cardnames.size());

		// filter out cards where their information is still relevant
		List<CardStapleInfo> cardsNotNeededAnymore = ioHandler.getCardsNotNeededAnymore(config.getRenewXdaysbefore());
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
					if(shouldDoCard(scryfallCard)) {
						CardStapleInfo cardStapleInfo = doCard(scryfallCard);
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
		formats = Arrays.asList(Format.values()).stream().filter(f -> config.getInterrestingFormats().contains(f))
				.filter(f -> f.getTop8Code() != null).map(f -> f.name()).collect(Collectors.toList());


//		CsvHandler csvHandler = new CsvHandler(new File("results/competitive_score.csv"));
//		csvHandler.init(fields);
		SqlHandler sqlHandler = new SqlHandler(
				config.getHost(),
				config.getPort(),
				config.getUser(),
				config.getPassword(),
				config.getDatabase(),
				config.getTable());
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
		ListObject<CardObject> foundCards = ScryfallApi.search(query).get();
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
		for (Format format : config.getInterrestingFormats()) {
			for (CompLevel compLevel : config.getCompLevels()) {
				String normalizedCardName = normalizeCardName(scryfallCard);
	
				Thread thread = new Thread(() -> {
					int deckCount = requestDeckCountMultiTry(normalizedCardName, format, compLevel, 3);
					cardStapleInfo.setFormatScore(format, deckCount * compLevel.getFactor());
				}, normalizedCardName + " in " + format);
				threads.add(thread);
				thread.start();
			}
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
        MtgTop8Search mtgTop8Search = new MtgTop8Search();
        mtgTop8Search.setBoard(config.isMainboard(), config.isMainboard());
        mtgTop8Search.setStartDate(config.getStartXdaysbefore());
        mtgTop8Search.setEndDate(config.getEndXdaysbefore());
        mtgTop8Search.setCards(normalizedCardName);
        mtgTop8Search.setCompLevel(compLevel);

        mtgTop8Search.setFormat(MtgTop8Format.valueOf(format.getTop8Code()));

        log.info(String.format("Requesting from mtgtop8; Card: '%s' Format: '%s'; CompLevel: '%s'",
                normalizedCardName, format, compLevel));
        return mtgTop8Search.getDeckCount();
    }

}
