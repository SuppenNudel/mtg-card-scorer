package de.rohmio.mtg.cardscorer.config;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;

public class ConfigTest {

	@Test
	public void mtgtop8ConfigTest() {
		MtgTop8Config mtgTop8Config = MtgTop8Config.loadConfig("@mtgtop8.config");
		assertEquals(Arrays.asList(MtgTop8Format.PIONEER, MtgTop8Format.MODERN, MtgTop8Format.LEGACY), mtgTop8Config.getFormats());
	}

}
