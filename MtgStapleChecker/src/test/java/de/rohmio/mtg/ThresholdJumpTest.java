package de.rohmio.mtg;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

public class ThresholdJumpTest {

	private String[] cardNames = {
		"Ancestral Vision",
		"Apostle's Blessing",
		"Blue Sun's Zenith",
		"Concealed Courtyard",
		"Creeping Tar Pit",
		"Dire Fleet Daredevil",
		"Dreadhorde Arcanist",
		"Elvish Clancaller",
		"Entreat the Angels",
		"Exquisite Firecraft",
		"Fiend Hunter",
		"Flooded Grove",
		"Liliana's Triumph",
		"Merrow Reejerey",
		"Myr Battlesphere",
		"Sin Collector",
		"Tasigur, the Golden Fang",
		"Viridian Corrupter"
	};
	
	@Test
	public void test() throws IOException {
		MtgStapleChecker.loadConfig();
		MtgStapleChecker.initScript();
		for(String cardName : cardNames) {
			Map<String, String> doCard = MtgStapleChecker.doCard(cardName);
			String modern = doCard.get("modern");
			String cardname = doCard.get("cardname");
		}
		
	}

}
