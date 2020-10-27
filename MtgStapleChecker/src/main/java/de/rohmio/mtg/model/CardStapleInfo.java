package de.rohmio.mtg.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.rohmio.scryfall.api.model.enums.Format;

public class CardStapleInfo {
	
	private String cardname;
	private Calendar timestamp;
	private Integer standard;
	private Integer pioneer;
	private Integer modern;
	private Integer legacy;
	private Integer pauper;
	private Integer vintage;
	private Integer commander;
	
	private Map<Format, Integer> formatScores = new HashMap<>();
	
	public CardStapleInfo(String cardname) {
		this.cardname = cardname;
	}
	
	@Override
	public String toString() {
		return cardname;
	}

	public boolean anyIsNull() {
		return standard == null
				|| pioneer == null
				|| modern == null
				|| legacy == null
				|| pauper == null
				|| vintage == null
				|| commander == null;
	}

	public void setFormatScore(Format format, int score) {
		formatScores.put(format, score);
	}
	
	public int getFormatScore(Format format) {
		return formatScores.get(format);
	}
	
	public Map<Format, Integer> getFormatScores() {
		return formatScores;
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
