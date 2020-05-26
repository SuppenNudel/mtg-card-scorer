package de.rohmio.mtg.model;

import java.util.Calendar;

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

	public boolean anyIsNull() {
		return standard == null
				|| pioneer == null
				|| modern == null
				|| legacy == null
				|| pauper == null
				|| vintage == null
				|| commander == null;
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

	public Integer getStandard() {
		return standard;
	}

	public void setStandard(Integer standard) {
		this.standard = standard;
	}

	public Integer getPioneer() {
		return pioneer;
	}

	public void setPioneer(Integer pioneer) {
		this.pioneer = pioneer;
	}

	public Integer getModern() {
		return modern;
	}

	public void setModern(Integer modern) {
		this.modern = modern;
	}

	public Integer getLegacy() {
		return legacy;
	}

	public void setLegacy(Integer legacy) {
		this.legacy = legacy;
	}

	public Integer getPauper() {
		return pauper;
	}

	public void setPauper(Integer pauper) {
		this.pauper = pauper;
	}

	public Integer getVintage() {
		return vintage;
	}

	public void setVintage(Integer vintage) {
		this.vintage = vintage;
	}

	public Integer getCommander() {
		return commander;
	}

	public void setCommander(Integer commander) {
		this.commander = commander;
	}

	public void setCardname(String cardname) {
		this.cardname = cardname;
	}
	
}
