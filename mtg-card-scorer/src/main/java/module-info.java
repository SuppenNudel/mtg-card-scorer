module de.rohmio.mtg.staplechecker {

	requires de.rohmio.mtg.scryfall.api;
	requires de.rohmio.mtg.mtgtop8.api;
	
	requires jcommander;
	requires java.logging;
	
	requires org.jooq;
	requires java.sql;
	
	requires org.junit.jupiter.api;
	
	opens de.rohmio.mtg.cardscorer.config to jcommander;
	opens de.rohmio.mtg.cardscorer.model;

}