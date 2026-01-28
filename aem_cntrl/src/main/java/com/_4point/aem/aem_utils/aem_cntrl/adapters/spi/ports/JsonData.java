package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface JsonData {

	/**
	 * Returns the JsonData as a String.
	 * 
	 * @return
	 */
	String asString();

	/**
	 * Determines the name of the root object
	 * 
	 * throws an exception of the root is an array.
	 * 
	 * @return
	 */
	default String determineRootName() {
		List<String> topLevelFieldNames = listChildren();
		if (topLevelFieldNames.size() != 1) {
			// Should only be one root object.
			throw new IllegalStateException("Expected just one json root but found nodes %s".formatted(topLevelFieldNames.toString()));
		}
		return topLevelFieldNames.get(0);
	}

	/**
	 * Returns the value of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonDataPtr
	 * @return
	 */
	Optional<String> at(JsonDataPointer jsonDataPtr);

	/**
	 * Returns the value of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default Optional<String> at(String jsonPtr){
		return at(pointerOf(jsonPtr));
	}

	/**
	 * Returns the JsonData of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default Optional<JsonData> subsetAt(String jsonPtr){
		return subsetAt(pointerOf(jsonPtr));
	}

	/**
	 * Returns the JsonData of the JsonNode pointed at using a JsonPointer.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	Optional<JsonData> subsetAt(JsonDataPointer jsonDataPtr);

	/**
	 * Returns a Stream of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonPtr - String containing JsonPointer
	 * @return
	 */
	default Stream<JsonData> arrayAt(String jsonPtr){
		return arrayAt(pointerOf(jsonPtr));
	}

	/**
	 * Returns a Stream of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonDataPtr - JsonPointer
	 * @return
	 */
	Stream<JsonData> arrayAt(JsonDataPointer jsonDataPtr);

	/**
	 * Returns a List of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default List<JsonData> listAt(String jsonPtr){
		return arrayAt(jsonPtr).toList();
	}

	/**
	 * Returns a List of JsonData of the Array pointed at using a JsonPointer
	 * 
	 * @param jsonDataPtr
	 * @return
	 */
	default List<JsonData> listAt(JsonDataPointer jsonDataPtr){
		return arrayAt(jsonDataPtr).toList();
	}

	/**
	 * Lists the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default List<String> listChildrenAt(String jsonPtr) {
		return listChildrenAt(pointerOf(jsonPtr));
	}

	/**
	 * Lists the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default List<String> listChildrenAt(JsonDataPointer jsonDataPtr) {
		return childrenAt(jsonDataPtr).toList();
	}

	/**
	 * Lists the names of the children of this object..
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default List<String> listChildren() {
		return children().toList();
	}

	/**
	 * Streams the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default Stream<String> childrenAt(String jsonPtr){
		return childrenAt(pointerOf(jsonPtr));
	}

	/**
	 * Streams the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	Stream<String> childrenAt(JsonDataPointer jsonDataPtr);

	/**
	 * Streams the names of the children of this object..
	 * 
	 * @param jsonPtr
	 * @return
	 */
	Stream<String> children();
	
	/**
	 * Returns true if there is a JsonNode pointed at by the JsonPointer string.
	 * 
	 * This routine works for normal Json data and Adaptive Form Json data.
	 * 
	 * If the JsonData is Adaptive Form Json data (i.e. it has the Adaptive Form wrapper), then it will locate the node
	 * using the "afBound" data node as the root node.  If that is not desired use the at(jsonPtr, false) call instead. 
	 * 
	 * @param jsonPtr
	 * @return
	 */
	default boolean hasNode(String jsonPtr){
		return hasNode(pointerOf(jsonPtr));
	}

	/**
	 * Returns true if there is a JsonNode pointed at by the JsonPointer string.
	 * 
	 * This routine works for normal Json data and Adaptive Form Json data.
	 * 
	 * If the JsonData is Adaptive Form Json data (i.e. it has the Adaptive Form wrapper), then it will locate the node
	 * using the "afBound" data node as the root node.  If that is not desired use the at(jsonPtr, false) call instead. 
	 * 
	 * @param jsonPtr
	 * @return
	 */
	boolean hasNode(JsonDataPointer jsonDataPtr);

	/**
	 * Inserts a property containing an object somewhere into the JSON
	 * 
	 * @param jsonPointer
	 * 		pointer to location where the property will be inserted
	 * @param property
	 * 		property to be inserted
	 * @param value
	 * 		json object of the property being inserted
	 * @return
	 * 		copy of the original JsonData with the property inserted.
	 */
	default JsonData insertJsonProperty(String jsonPointer, String property, JsonData value){
		return insertJsonProperty(pointerOf(jsonPointer), property, value);
	}

	/**
	 * Inserts a property with a String value somewhere into the JSON
	 * 
	 * @param jsonPointer
	 * 		pointer to location where the property will be inserted
	 * @param property
	 * 		property to be inserted
	 * @param value
	 * 		value of the property being inserted
	 * @return
	 * 		copy of the original JsonData with the property inserted.
	 */
	default JsonData insertJsonProperty(String jsonPointer, String property, String value){
		return insertJsonProperty(pointerOf(jsonPointer), property, value);
	}

	/**
	 * Inserts a property containing an object somewhere into the JSON
	 * 
	 * @param jsonPointer
	 * 		pointer to location where the property will be inserted
	 * @param property
	 * 		property to be inserted
	 * @param value
	 * 		json object of the property being inserted
	 * @return
	 * 		copy of the original JsonData with the property inserted.
	 */
	JsonData insertJsonProperty(JsonDataPointer jsonDataPointer, String property, JsonData value);

	/**
	 * Inserts a property with a String value somewhere into the JSON
	 * 
	 * @param jsonPointer
	 * 		pointer to location where the property will be inserted
	 * @param property
	 * 		property to be inserted
	 * @param value
	 * 		value of the property being inserted
	 * @return
	 * 		copy of the original JsonData with the property inserted.
	 */
	JsonData insertJsonProperty(JsonDataPointer jsonDataPointer, String property, String value);

	/**
	 * Create a JsonDataPointer from a string.
	 * 
	 * @param pointerString
	 * 		Json Pointer string to location within the Json 
	 * @return
	 *      pointer to location within the Json
	 */
	JsonDataPointer pointerOf(String pointerString);

	/**
	 * Create a JsonData object from a string.
	 * 
	 * @param jsonString String containing the JSON data
	 * @return JsonData object containing the JSON data
	 */
	@FunctionalInterface
	public static interface JsonDataFactory extends Function<String, JsonData> {}
	
	/**
	 * A Pointer to a location in the JSON data.
	 */
	public static interface JsonDataPointer {}

	@SuppressWarnings("serial")
	public static class JsonDataException extends RuntimeException {

		public JsonDataException() {
		}

		public JsonDataException(String message, Throwable cause) {
			super(message, cause);
		}

		public JsonDataException(String message) {
			super(message);
		}

		public JsonDataException(Throwable cause) {
			super(cause);
		}
	}
}