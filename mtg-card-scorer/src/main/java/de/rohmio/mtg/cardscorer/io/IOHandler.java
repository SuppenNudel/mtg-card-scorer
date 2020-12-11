package de.rohmio.mtg.cardscorer.io;

import java.util.Collection;
import java.util.List;

import de.rohmio.mtg.cardscorer.model.CardStapleInfo;

public interface IOHandler {

	void addDataset(CardStapleInfo cardStapleInfo);
	CardStapleInfo getCardStapleInfo(String cardname);
	List<CardStapleInfo> getCardsNotNeeded(int daysAgo);
	List<CardStapleInfo> getCardStapleInfos(Collection<String> cardnames);

}
