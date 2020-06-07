package de.rohmio.mtg.write;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.rohmio.mtg.model.CardStapleInfo;

public interface IOHandler {

	public void init(List<String> titles) throws IOException;
	public void addDataset(Map<String, String> values) throws IOException;
	public CardStapleInfo getCardStapleInfo(String cardname);
	public List<CardStapleInfo> getCardsNotNeededAnymore(int daysAgo);
	
}
