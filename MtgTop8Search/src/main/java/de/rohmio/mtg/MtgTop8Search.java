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
	
	private static final String DATA_CARDS = "cards";
	
	private static final String ln = System.lineSeparator();
	
	public static void main(String[] args) throws IOException {
		CompLevel[] compLevels = { CompLevel.Professional, CompLevel.Major };
		String[] cards = { "Leyline of the Void" };
		getDecksContainingCards("LE", true, true, 90, cards, compLevels);
	}
	

	public static List<DeckInfo> getDecksContainingCard(String format, boolean mainboard, boolean sideboard, int lastXdays, String card, CompLevel... compLevels) throws IOException {
		return getDecksContainingCards(format, mainboard, sideboard, lastXdays, new String[] { card }, compLevels);
	}
	
	public static List<DeckInfo> getDecksContainingCards(String format, boolean mainboard, boolean sideboard, int lastXdays, String[] cards, CompLevel... compLevels) throws IOException {
		Connection connection = Jsoup.connect("https://www.mtgtop8.com/search");
		
		setCards(connection, cards);
		setCompLevel(connection, compLevels);
		setBoard(connection, mainboard, sideboard);
		setFormat(connection, format);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_WEEK, -lastXdays);
		Date result = cal.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");
		
		setStartDate(connection, dateFormat.format(result));
		
		Document doc = connection.post();
		List<DeckInfo> deckInfos = parseDocument(doc);
		return deckInfos;
	}
	
	private static void setStartDate(Connection connection, String date) {
		System.out.println("Start Date: "+date);
		connection.data("date_start", date);
	}
	
	private static void setFormat(Connection connection, String format) {
		System.out.println("Format: "+format);
		connection.data("format", format);
	}
	
	private static void setBoard(Connection connection, boolean main, boolean side) {
		if(main) {
			System.out.println("Maindeck");
			connection.data("MD_check", "1");
		}
		if(side) {
			System.out.println("Sideboard");
			connection.data("SB_check", "1");
		}
	}
	
	private static void setCompLevel(Connection connection, CompLevel... compLevels) {
		System.out.println("Comp. Levels: "+Arrays.asList(compLevels));
		for(CompLevel compLevel : compLevels) {
			connection.data(String.format("compet_check[%s]", compLevel.getTop8Code()), "1");
		}
	}
	
	private static void setCards(Connection connection, String... cards) {
		System.out.println("Cards: "+Arrays.asList(cards));
		connection.data(DATA_CARDS, String.join(ln, cards));
	}
	
	private static List<DeckInfo> parseDocument(Document doc) {
		String sum = doc.select("table > tbody > tr > td > div[class=w_title]").text();
		System.out.println("Sum: "+sum);
		
		Elements deckRowElements = doc.select("table > tbody > tr[class=hover_tr]");
		List<DeckInfo> deckInfos = new ArrayList<>();
		for(Element deckRowElement : deckRowElements) {
			Elements select = deckRowElement.select("td");
			String deckName = select.get(1).text();
			Elements stars = select.get(4).select("img");
			String levelStar = stars.attr("src");
			String level = null;
			switch (levelStar) {
			case "/graph/bigstar.png":
				level = "Professional";
				break;
			case "/graph/star.png":
				switch (stars.size()) {
				case 1:
					level = "Regular";
					break;
				case 2:
					level = "Competitive";
					break;
				case 3:
					level = "Major";
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
