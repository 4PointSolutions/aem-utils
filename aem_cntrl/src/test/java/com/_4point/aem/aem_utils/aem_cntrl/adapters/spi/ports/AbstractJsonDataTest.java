package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Function;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com._4point.testing.matchers.javalang.ExceptionMatchers;
import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData.JsonDataException;

public abstract class AbstractJsonDataTest {

	private static final String OBJ_SUBTREE = "{ \"subObj\" : \"subObjValue\"}";
	private static final String SAMPLE_JSON_FORMAT_STR = "{ \"foo\" : \"bar\", \"obj\" : %s }";
	private static final String SAMPLE_JSON_DATA = String.format(SAMPLE_JSON_FORMAT_STR, OBJ_SUBTREE);
	
	private final Function<String, JsonData> jsonFactory;
	private final JsonData underTest;

	public AbstractJsonDataTest(Function<String, JsonData> jsonFactory) {
		this.jsonFactory = jsonFactory;
		this.underTest = jsonFactory.apply(SAMPLE_JSON_DATA);
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testAsString() {
		assertSame(SAMPLE_JSON_DATA, underTest.asString());
	}

	@Test
	void testHasNodeString() {
		assertAll(
				()->assertTrue(underTest.hasNode("/foo"), "Should find foo in JsonData"),
				()->assertTrue(underTest.hasNode("/obj/subObj"), "Should find obj/subObj in JsonData"),
				()->assertFalse(underTest.hasNode("/bar"), "Should not find bar in JsonData")
				);
	}

	@Test
	void testAtString() {
		assertAll(
				()->assertEquals("bar", underTest.at("/foo").get(), "Should find foo in JsonData"),
				()->assertEquals("subObjValue", underTest.at("/obj/subObj").get(), "Should find obj/subObj in JsonData"),
				()->assertTrue(underTest.at("/bar").isEmpty(), "Should not find bar in JsonData")
				);
	}

	@Test
	void testSubsetAtString() throws Exception {
		JSONAssert.assertEquals(OBJ_SUBTREE , underTest.subsetAt("/obj").get().asString(), false);
		assertTrue(underTest.subsetAt("/foo").isEmpty());
	}

	@Test
	void testFrom_InvalidJson() {
		JsonDataException ex = assertThrows(JsonDataException.class, ()->jsonFactory.apply("{ foo : bar }"));	// labels not in quotes.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while parsing JsonData string"));
	}

	@Test
	void testInsertJsonProperty_JsonData() throws Exception {
		JsonData result = underTest.insertJsonProperty("/obj", "subSubObj", jsonFactory.apply(OBJ_SUBTREE));
		JSONAssert.assertEquals(String.format(SAMPLE_JSON_FORMAT_STR, String.format("{ \"subObj\" : \"subObjValue\", \"subSubObj\" : %s }", OBJ_SUBTREE)) , result.asString(), false);
	}

	@Test
	void testInsertJsonProperty_String() throws Exception {
		JsonData result = underTest.insertJsonProperty("/obj", "property", "value");
		JSONAssert.assertEquals(String.format(SAMPLE_JSON_FORMAT_STR, "{ \"property\" : \"value\" }") , result.asString(), false);
	}

	@Test
	void testListAtString() throws Exception {
		String obj1 = "{ \"obj1\" : \"obj1Value\" }";
		String obj2 = "{ \"obj2\" : \"obj2Value\" }";
		String sampleArray = "[ %s, %s ]".formatted(obj1, obj2);
		JsonData underTest = jsonFactory.apply(String.format(SAMPLE_JSON_FORMAT_STR, sampleArray));
		List<JsonData> result = underTest.listAt("/obj");
		
		assertEquals(2, result.size());
		JSONAssert.assertEquals(obj1 , result.get(0).asString(), false);
		JSONAssert.assertEquals(obj2 , result.get(1).asString(), false);
	}

	@Test
	void testListAtJsonPointer() throws Exception {
		String obj1 = "{ \"obj1\" : \"obj1Value\" }";
		String obj2 = "{ \"obj2\" : \"obj2Value\" }";
		String sampleArray = "[ %s, %s ]".formatted(obj1, obj2);
		JsonData underTest = jsonFactory.apply(String.format(SAMPLE_JSON_FORMAT_STR, sampleArray));
		List<JsonData> result = underTest.listAt(underTest.pointerOf("/obj"));
		
		assertEquals(2, result.size());
		JSONAssert.assertEquals(obj1 , result.get(0).asString(), false);
		JSONAssert.assertEquals(obj2 , result.get(1).asString(), false);
	}

	@Test
	void testDetermineRootName() {
		JsonData underTest = jsonFactory.apply("{ \"foo\" : %s }".formatted(OBJ_SUBTREE));
		assertEquals("foo", underTest.determineRootName());
	}

	@Test
	void testDetermineRootName_TooManyRoots() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.determineRootName());
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Expected just one json root but found nodes", "foo", "obj"));
	}

	@Test
	void testListChildren() {
		assertEquals(List.of("foo", "obj"), underTest.listChildren());
	}

	@Test
	void testListChildrenAtString() {
		assertEquals(List.of("subObj"), underTest.listChildrenAt("/obj"));
	}

	@Test
	void testListChildrenAtJsonDataPointer() {
		assertEquals(List.of("subObj"), underTest.listChildrenAt(underTest.pointerOf("/obj")));
	}

	@Test
	void testChildren() {
		assertEquals(List.of("foo", "obj"), underTest.children().toList());
	}

	@Test
	void testChildrenAtString() {
		assertEquals(List.of("subObj"), underTest.childrenAt("/obj").toList());
	}

	@Test
	void testChildrenAtJsonDataPointer() {
		assertEquals(List.of("subObj"), underTest.childrenAt(underTest.pointerOf("/obj")).toList());
	}

	@Test
	void testArrayAtString() throws JSONException {
		String obj1 = "{ \"obj1\" : \"obj1Value\" }";
		String obj2 = "{ \"obj2\" : \"obj2Value\" }";
		String sampleArray = "[ %s, %s ]".formatted(obj1, obj2);
		JsonData underTest = jsonFactory.apply(String.format(SAMPLE_JSON_FORMAT_STR, sampleArray));
				List<JsonData> result = underTest.listAt(underTest.pointerOf("/obj"));
		
		assertEquals(2, result.size());
		JSONAssert.assertEquals(obj1 , result.get(0).asString(), false);
		JSONAssert.assertEquals(obj2 , result.get(1).asString(), false);
	}

	@Test
	void testArrayAtString_NotArray() {
		assertEquals(List.of(), underTest.arrayAt("/obj").map(JsonData::asString).toList());
	}

	@Test
	void testPointerToString() {
		String pointerValue = "/obj/subObj";
		assertEquals(pointerValue, underTest.pointerOf(pointerValue).toString());
	}
}