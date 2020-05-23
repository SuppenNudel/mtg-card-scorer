package de.rohmio.mtg;

public class SetMapping {
	
	public static String scryfallToGoldfish(String edition) {
		String regex = "[,':\\.]|// ";
		edition = edition.replaceAll(regex, "");
		switch (edition) {
		case "Modern Masters 2017": return "Modern Masters 2017 Edition";
		case "Mystery Booster Retail Edition Foils": return "Mystery Booster Retail Edition Foils:Foil";
		case "Magic 2014": return "Magic 2014 Core Set";
		case "Magic 2015": return "Magic 2015 Core Set";
		case "Commander 2011": return "Commander";
		case "Commander 2013": return "Commander 2013 Edition";
		case "Commander 2014": return "Commander 2014 Edition";
		case "Planechase 2012": return "Planechase 2012 Edition";
		default: return edition;
		}
	}

}
