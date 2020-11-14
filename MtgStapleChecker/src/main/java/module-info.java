module de.rohmio.mtg.staplechecker {

	requires de.rohmio.mtg.scryfall.api;
	requires de.rohmio.mtg.mtgtop8.api;
	requires org.apache.commons.configuration2;
	requires java.logging;
	requires java.sql;
	requires org.jooq;

//	requires org.junit.jupiter.api;

	exports de.rohmio.mtg.staplechecker;
	exports de.rohmio.mtg.staplechecker.model;
	exports de.rohmio.mtg.staplechecker.io;


}