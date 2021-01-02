package de.rohmio.mtg.cardscore.database;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.LOCALDATETIME;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.jooq.Field;
import org.jooq.impl.DSL;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;

public class CardStapleInfo {

	public static final Field<String> CARDNAME = DSL.field("cardname", VARCHAR.nullable(false));
	public static final Field<LocalDateTime> TIMESTAMP = DSL.field("timestamp", LOCALDATETIME.nullable(false));
	public static final Field<Integer> STANDARD = DSL.field("standard", INTEGER);
	public static final Field<Integer> PIONEER = DSL.field("pioneer", INTEGER);
	public static final Field<Integer> MODERN = DSL.field("modern", INTEGER);
	public static final Field<Integer> LEGACY = DSL.field("legacy", INTEGER);
	public static final Field<Integer> PAUPER = DSL.field("pauper", INTEGER);
	public static final Field<Integer> VINTAGE = DSL.field("vintage", INTEGER);
	public static final Field<Integer> COMMANDER = DSL.field("commander", INTEGER);

	public static final Map<MtgTop8Format, Field<Integer>> FORMAT_FIELD = new HashMap<>();

	static {
		FORMAT_FIELD.put(MtgTop8Format.STANDARD, STANDARD);
		FORMAT_FIELD.put(MtgTop8Format.PIONEER, PIONEER);
		FORMAT_FIELD.put(MtgTop8Format.MODERN, MODERN);
		FORMAT_FIELD.put(MtgTop8Format.LEGACY, LEGACY);
		FORMAT_FIELD.put(MtgTop8Format.PAUPER, PAUPER);
		FORMAT_FIELD.put(MtgTop8Format.VINTAGE, VINTAGE);
		FORMAT_FIELD.put(MtgTop8Format.MTGO_COMMANDER, COMMANDER);
	}

	private String cardname;
	private LocalDateTime timestamp;

	private Map<MtgTop8Format, Integer> formatScores = new HashMap<>();

	@SuppressWarnings("unused")
	private CardStapleInfo() {}

	public CardStapleInfo(String cardname) {
		this.cardname = cardname;
	}

	@Override
	public String toString() {
		return String.format("%s (%s) - %s", cardname, timestamp, formatScores);
	}

	public boolean anyIsNull() {
		return formatScores.values().stream().anyMatch(score -> score == null);
	}

	public void setFormatScore(MtgTop8Format format, Integer score) {
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

	public Integer getFormatScore(MtgTop8Format format) {
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
		setFormatScore(MtgTop8Format.STANDARD, score);
	}

	public void setPioneer(Integer score) {
		setFormatScore(MtgTop8Format.PIONEER, score);
	}

	public void setModern(Integer score) {
		setFormatScore(MtgTop8Format.MODERN, score);
	}

	public void setLegacy(Integer score) {
		setFormatScore(MtgTop8Format.LEGACY, score);
	}

	public void setPauper(Integer score) {
		setFormatScore(MtgTop8Format.PAUPER, score);
	}

	public void setVintage(Integer score) {
		setFormatScore(MtgTop8Format.VINTAGE, score);
	}

	public void setCommander(Integer score) {
		setFormatScore(MtgTop8Format.MTGO_COMMANDER, score);
	}

	public Integer getStandard() {
		return getFormatScore(MtgTop8Format.STANDARD);
	}

	public Integer getPioneer() {
		return getFormatScore(MtgTop8Format.PIONEER);
	}

	public Integer getModern() {
		return getFormatScore(MtgTop8Format.MODERN);
	}

	public Integer getLegacy() {
		return getFormatScore(MtgTop8Format.LEGACY);
	}

	public Integer getPauper() {
		return getFormatScore(MtgTop8Format.PAUPER);
	}

	public Integer getVintage() {
		return getFormatScore(MtgTop8Format.VINTAGE);
	}

	public Integer getCommander() {
		return getFormatScore(MtgTop8Format.MTGO_COMMANDER);
	}

}
