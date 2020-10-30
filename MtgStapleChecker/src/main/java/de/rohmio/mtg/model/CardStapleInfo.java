package de.rohmio.mtg.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.rohmio.scryfall.api.model.enums.Format;

public class CardStapleInfo {
	
	private String cardname;
	private Calendar timestamp;
	
	private Map<Format, Integer> formatScores = new HashMap<>();
	
	public CardStapleInfo(String cardname) {
		this.cardname = cardname;
	}
	
	@Override
	public String toString() {
		return cardname;
	}

	public boolean anyIsNull() {
		return formatScores.values().stream().anyMatch(i -> i == null);
	}

	public void setFormatScore(Format format, int score) {
		formatScores.put(format, score);
	}
	
	public int getFormatScore(Format format) {
		return formatScores.get(format);
	}
	
	public String getCardname() {
		return cardname;
	}

	public Calendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

}
