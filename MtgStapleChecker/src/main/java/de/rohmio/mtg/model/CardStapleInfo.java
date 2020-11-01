package de.rohmio.mtg.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.rohmio.scryfall.api.model.enums.Format;

public class CardStapleInfo {
	
	private String cardname;
	private Calendar timestamp;
	private Map<Format, Integer> formatScores;
	
	public CardStapleInfo(String cardname) {
		formatScores = new HashMap<>();
		this.cardname = cardname;
		if(timestamp == null) {
			timestamp = Calendar.getInstance();
		}
	}
	
	protected CardStapleInfo(String cardname, Calendar timestamp, int standard, int pioneer, int modern, int legacy, int pauper, int vintage, int commander) {
		this(cardname);
		this.timestamp = timestamp;
		formatScores.put(Format.standard, standard);
		formatScores.put(Format.pioneer, pioneer);
		formatScores.put(Format.modern, modern);
		formatScores.put(Format.legacy, legacy);
		formatScores.put(Format.pauper, pauper);
		formatScores.put(Format.vintage, vintage);
		formatScores.put(Format.commander, commander);
	}

	@Override
	public String toString() {
		return cardname;
	}

	public boolean anyIsNull() {
		return formatScores.values().stream().anyMatch(score -> score == null);
	}

	public void setFormatScore(Format format, int score) {
		formatScores.put(format, score);
	}

	public Integer getFormatScore(Format format) {
		return formatScores.get(format);
	}

	public String getCardname() {
		return cardname;
	}

	public Calendar getTimestamp() {
		return timestamp;
	}

}
