//DEPS com.thedeanda:lorem:2.2

package com._4point.aem.aem_utils.aem_cntrl.domain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

public class LoremIpsumGenerator {

	public static void main(String[] args) throws Exception {
		List<String> argList = Arrays.asList(args);
		// If "useInput" provided, then copy input to the output first, before generating lorem ipsum
		if (argList.size() > 0 && argList.get(0).equals("useInput") ) {
			try(var reader = new InputStreamReader(System.in); var bufferedReader = new BufferedReader(reader)) {
				bufferedReader.lines().forEach(System.out::println);
			}
		}
		
		// Any input has been written, so generate some nonesense that will exceed the pipe limit.
		Lorem lorem = LoremIpsum.getInstance();
		String paragraphs = lorem.getParagraphs(39, 40);
		System.out.println(paragraphs);
		
		// System.err.println("This should generate a test failure");
	}

}
