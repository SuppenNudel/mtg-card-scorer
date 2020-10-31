package de.rohmio.mtg;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MtgTop8Search {
	
	/*
		current_page: 
		event_titre: 
		deck_titre: 
		player: 
		format: 
		archetype_sel[VI]: 
		archetype_sel[LE]: 
		archetype_sel[MO]: 
		archetype_sel[PI]: 
		archetype_sel[EX]: 
		archetype_sel[ST]: 
		archetype_sel[BL]: 
		archetype_sel[PAU]: 
		archetype_sel[EDH]: 
		archetype_sel[HIGH]: 
		archetype_sel[EDHP]: 
		archetype_sel[CHL]: 
		archetype_sel[PEA]: 
		archetype_sel[EDHM]: 
		compet_check[P]: 1
		compet_check[M]: 1
		compet_check[C]: 1
		compet_check[R]: 1
		MD_check: 1
		SB_check: 1
		cards: Lightning Bolt
		date_start: 
		date_end: 
	 */
	
	private static final String ln = System.lineSeparator();
	
	private MtgTop8Format format;
	private static final String f_format = "format";
	
	private boolean mainboard;
	private static final String f_mainboard = "MD_check";
	
	private boolean sideboard;
	private static final String f_sideboard = "SB_check";

	private String startDate;
	private static final String f_startdate = "date_start";
	
	private String endDate;
	private static final String f_enddate = "date_end";
	
	private String[] cards;
	private static final String f_cards = "cards";
	
	private CompLevel[] compLevels;
	private static final String f_complevels = "compet_check[%s]";
	
	private static final String f_currentPage = "current_page";

	@Override
	public String toString() {
		return Arrays.asList(cards).toString();
	}
	
	public String[] getCards() {
		return cards;
	}
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	
	public void setStartDate(int lastXdays) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -lastXdays);
		Date result = cal.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");
		
		setStartDate(dateFormat.format(result));
	}
	
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	
	public void setEndDate(int lastXdays) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -lastXdays);
		Date result = cal.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");
		
		setEndDate(dateFormat.format(result));
	}
	
	public void setFormat(MtgTop8Format format) {
		this.format = format;
	}
	
	public void setBoard(boolean mainboard, boolean sideboard) {
		this.mainboard = mainboard;
		this.sideboard = sideboard;
	}
	
	public void setCompLevel(CompLevel... compLevels) {
		this.compLevels = compLevels;
	}
	
	public void setCards(String... cards) {
		this.cards = cards;
	}
	
	private List<DeckInfo> getDecks(int page) throws IOException {
		Document doc = makeRequest(page);
		List<DeckInfo> deckInfos = parseDocument(doc);
		return deckInfos;
	}
	
	private Document makeRequest(int page) throws IOException {
		Connection connection = Jsoup.connect("https://www.mtgtop8.com/search");
		connection.postDataCharset("ISO-8859-1");
		
		if(format != null) {
			connection.data(f_format, format.name());
		}
		if(startDate != null) {
			connection.data(f_startdate, startDate);
		}
		if(endDate != null) {
			connection.data(f_enddate, endDate);
		}
		if(compLevels != null) {
			for(CompLevel compLevel : compLevels) {
				connection.data(String.format(f_complevels, compLevel.getTop8Code()), "1");
			}
		}
		if(cards != null) {
			List<String> cardnames = new ArrayList<>();
			for(String cardname : cards) {
				cardnames.add(cardname);
			}
			connection.data(f_cards, String.join(ln, cardnames));
		}
		if(mainboard) {
			connection.data(f_mainboard, "1");
		}
		if(sideboard) {
			connection.data(f_sideboard, "1");
		}
		connection.data(f_currentPage, String.valueOf(page));
		
		Document doc = connection.post();
		return doc;
	}
	
	public int getDeckCount() throws IOException {
		Document doc = makeRequest(0);
		int deckCount = parseDocumentForDeckCount(doc);
		return deckCount;
	}
	
	private int parseDocumentForDeckCount(Document doc) {
		if(doc.toString().contains("No match for")) {
			return 0;
		}
		String sumText = doc.select("table > tbody > tr > td > div[class=w_title]").text();
		sumText = sumText.replace("decks matching", "").trim();
		
		int allDecksCount = Integer.parseInt(sumText);
		this.expectedDeckCount = allDecksCount;
		return allDecksCount;
	}
	
	@Deprecated
	public List<DeckInfo> getDecks() throws IOException {
		List<DeckInfo> allDecks = new ArrayList<>();
		for(int page=1; expectedDeckCount == null || expectedDeckCount > allDecks.size(); ++page) {
			List<DeckInfo> decks = getDecks(page);
			allDecks.addAll(decks);
		}
		expectedDeckCount = null;
		return allDecks;
	}
	
	private Integer expectedDeckCount = null;
	
	private List<DeckInfo> parseDocument(Document doc) {
		String sumText = doc.select("table > tbody > tr > td > div[class=w_title]").text();
		sumText = sumText.replace("decks matching", "").trim();
		
		int allDecksCount = Integer.parseInt(sumText);
		this.expectedDeckCount = allDecksCount; 
		
		Elements deckRowElements = doc.select("table > tbody > tr[class=hover_tr]");
		List<DeckInfo> deckInfos = new ArrayList<>();
		for(Element deckRowElement : deckRowElements) {
			Elements select = deckRowElement.select("td");
			String deckName = select.get(1).text();
			Elements stars = select.get(4).select("img");
			String levelStar = stars.attr("src");
			CompLevel level = null;
			switch (levelStar) {
			case "/graph/bigstar.png":
				level = CompLevel.Professional;
				break;
			case "/graph/star.png":
				switch (stars.size()) {
				case 1:
					level = CompLevel.Regular;
					break;
				case 2:
					level = CompLevel.Competitive;
					break;
				case 3:
					level = CompLevel.Major;
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			
			String rank = select.get(5).text();
			String date = select.get(6).text();
			
			DeckInfo deckInfo = new DeckInfo(deckName, level, rank, date);
			deckInfos.add(deckInfo);
		}
		return deckInfos;
	}

}
