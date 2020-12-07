package de.rohmio.mtg.cardscorer.config;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.rohmio.mtg.scryfall.api.model.Format;

public class ConfigTest {

	@Test
	public void mtgtop8ConfigTest() {
		MtgTop8Config mtgTop8Config = MtgTop8Config.loadConfig("@mtgtop8.config");
		assertEquals(Arrays.asList(Format.pioneer, Format.modern, Format.legacy), mtgTop8Config.getFormats());
	}

}
