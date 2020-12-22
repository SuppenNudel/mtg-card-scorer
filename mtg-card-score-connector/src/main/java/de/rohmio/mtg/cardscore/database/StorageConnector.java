package de.rohmio.mtg.cardscore.database;

import java.util.Collection;
import java.util.List;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;

public interface StorageConnector {

	void addDataset(CardStapleInfo cardStapleInfo);
	CardStapleInfo getCardStapleInfo(String cardname);
	List<CardStapleInfo> getCardStapleInfos(Collection<String> cardnames);
	List<CardStapleInfo> getCardsNotNeeded(int daysAgo, List<MtgTop8Format> formats);

}
