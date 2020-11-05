package de.rohmio.mtg.io;

import java.io.IOException;
import java.util.List;

import de.rohmio.mtg.model.CardStapleInfo;

public interface IOHandler {

	String FIELD_CARDNAME = "cardname";
	String FIELD_TIMESTAMP = "timestamp";

	void init() throws IOException;
	void addDataset(CardStapleInfo cardStapleInfo) throws IOException;
	CardStapleInfo getCardStapleInfo(String cardname);
	List<CardStapleInfo> getCardsNotNeeded(int daysAgo);

}
