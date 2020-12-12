package de.rohmio.mtg.cardscorer.model;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.LOCALDATETIME;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.jooq.Field;
import org.jooq.impl.DSL;

import de.rohmio.mtg.scryfall.api.model.Format;

public class CardStapleInfo {

	public static final Field<String> CARDNAME = DSL.field("cardname", VARCHAR);
	public static final Field<LocalDateTime> TIMESTAMP = DSL.field("timestamp", LOCALDATETIME);
	public static final Field<Integer> STANDARD = DSL.field("standard", INTEGER);
	public static final Field<Integer> PIONEER = DSL.field("pioneer", INTEGER);
	public static final Field<Integer> MODERN = DSL.field("modern", INTEGER);
	public static final Field<Integer> LEGACY = DSL.field("legacy", INTEGER);
	public static final Field<Integer> PAUPER = DSL.field("pauper", INTEGER);
	public static final Field<Integer> VINTAGE = DSL.field("vintage", INTEGER);
	public static final Field<Integer> COMMANDER = DSL.field("commander", INTEGER);
	
	public static final Map<Format, Field<Integer>> FORMAT_FIELD = new HashMap<>();
	
	static {
		FORMAT_FIELD.put(Format.standard, STANDARD);
		FORMAT_FIELD.put(Format.pioneer, PIONEER);
		FORMAT_FIELD.put(Format.modern, MODERN);
		FORMAT_FIELD.put(Format.legacy, LEGACY);
		FORMAT_FIELD.put(Format.pauper, PAUPER);
		FORMAT_FIELD.put(Format.vintage, VINTAGE);
		FORMAT_FIELD.put(Format.commander, COMMANDER);
	}

	private String cardname;
	private LocalDateTime timestamp;
	
	private Map<Format, Integer> formatScores = new HashMap<>();
	
	@SuppressWarnings("unused")
	private CardStapleInfo() {}

	public CardStapleInfo(String cardname) {
		this.cardname = cardname;
	}

	@Override
	public String toString() {
		return cardname + " - " + formatScores;
	}

	public boolean anyIsNull() {
		return formatScores.values().stream().anyMatch(score -> score == null);
	}

	public void setFormatScore(Format format, Integer score) {
//		switch (format) {
//		case standard: standard = score; break;
//		case pioneer: pioneer = score; break;
//		case modern: modern = score; break;
//		case legacy: legacy = score; break;
//		case pauper: pauper = score; break;
//		case vintage: vintage = score; break;
//		case commander: commander = score; break;
//		default: throw new RuntimeException("Not implemented format: "+format);
//		}
		formatScores.put(format, score);
	}

	public Integer getFormatScore(Format format) {
		return formatScores.get(format);
	}

	public String getCardName() {
		return cardname;
	}

	public void setCardName(String cardName) {
		this.cardname = cardName;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public void setStandard(Integer score) {
		setFormatScore(Format.standard, score);
	}

	public void setPioneer(Integer score) {
		setFormatScore(Format.pioneer, score);
	}

	public void setModern(Integer score) {
		setFormatScore(Format.modern, score);
	}

	public void setLegacy(Integer score) {
		setFormatScore(Format.legacy, score);
	}

	public void setPauper(Integer score) {
		setFormatScore(Format.pauper, score);
	}

	public void setVintage(Integer score) {
		setFormatScore(Format.vintage, score);
	}

	public void setCommander(Integer score) {
		setFormatScore(Format.commander, score);
	}

	public Integer getStandard() {
		return getFormatScore(Format.standard);
	}

	public Integer getPioneer() {
		return getFormatScore(Format.pioneer);
	}

	public Integer getModern() {
		return getFormatScore(Format.modern);
	}

	public Integer getLegacy() {
		return getFormatScore(Format.legacy);
	}

	public Integer getPauper() {
		return getFormatScore(Format.pauper);
	}

	public Integer getVintage() {
		return getFormatScore(Format.vintage);
	}
	
	public Integer getCommander() {
		return getFormatScore(Format.commander);
	}
	
}
