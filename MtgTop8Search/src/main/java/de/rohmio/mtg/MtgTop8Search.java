package de.rohmio.mtg;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
	
	private String[] cards;
	private static final String f_cards = "cards";
	
	private CompLevel[] compLevels;
	private static final String f_complevels = "compet_check[%s]";
	
	private static final String f_currentPage = "current_page";
	
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
		Connection connection = Jsoup.connect("https://www.mtgtop8.com/search");
		
		connection.data(f_format, format.getTop8Code());
		connection.data(f_startdate, startDate);
		for(CompLevel compLevel : compLevels) {
			connection.data(String.format(f_complevels, compLevel.getTop8Code()), "1");
		}
		connection.data(f_cards, String.join(ln, cards));
		if(mainboard) {
			connection.data(f_mainboard, "1");
		}
		if(sideboard) {
			connection.data(f_sideboard, "1");
		}
		connection.data(f_currentPage, String.valueOf(page));
		
		Document doc = connection.post();
		
		/* 
		 * TODO calculate score by multiplying the count of decks with their comp level
		 * e.g.
		 * 4 professional	=> 4*4
		 * 3 major decks	=> 3*3
		 * 16 competitive	=> 16*2
		 * 20 regular		=> 20*1
		 */
		
		List<DeckInfo> deckInfos = parseDocument(doc);
		return deckInfos;
	}
	
	public List<DeckInfo> getDecks() throws IOException {
		List<DeckInfo> allDecks = new ArrayList<>();
		for(int page=0; page<100; ++page) {
			List<DeckInfo> decks = getDecks(page);
			allDecks.addAll(decks);
		}
		int sum = allDecks.size();
		return allDecks;
	}
	
	public List<DeckInfo> parseDocument(Document doc) {
		String sumText = doc.select("table > tbody > tr > td > div[class=w_title]").text();
		sumText = sumText.replace(" decks matching", "");
		int sum = Integer.parseInt(sumText);
		
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
