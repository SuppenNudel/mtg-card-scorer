package de.rohmio.mtg;

public class DeckInfo {

	private String deckName;
	private CompLevel level; // competitiveness
	private String rank; // result
	private String date;
	
	public DeckInfo(String deckName, CompLevel level, String rank, String date) {
		this.deckName = deckName;
		this.level = level;
		this.rank = rank;
		this.date = date;
	}
	
	@Override
	public String toString() {
		return String.format("Deckname: %s; Level: %s; Rank: %s; Date: %s", deckName, level, rank, date);
	}
	
	public String getDeckName() {
		return deckName;
	}
	public String getDate() {
		return date;
	}
	public CompLevel getLevel() {
		return level;
	}
	public String getRank() {
		return rank;
	}
	
}
