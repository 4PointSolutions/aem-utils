package com._4point.aem.aem_utils.aem_cntrl.adapters.spi.adapters;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com._4point.aem.aem_utils.aem_cntrl.adapters.spi.ports.JsonData;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class is used to store Json Data.
 * 
 * Internally it stores it as a String.
 *
 */
/**
 * 
 */
public class JacksonJsonData implements JsonData {
	// ObjectMapper is expensive to create but is threadsafe, so only create it once.
	// See: https://stackoverflow.com/questions/57670466/objectmapper-best-practice-for-thread-safety-and-performance
	// Also see: https://stackoverflow.com/questions/3907929/should-i-declare-jacksons-objectmapper-as-a-static-field
	private static final ObjectMapper mapper = new ObjectMapper();

	private final String jsonData;
	private final JsonNode rootNode;
	
	
	private JacksonJsonData(String jsonData, JsonNode rootNode) {
		this.jsonData = jsonData;
		this.rootNode = rootNode;
	}
	
	/**
	 * Returns the JsonData as a String.
	 * 
	 * @return
	 */
	@Override
	public String asString() {
		return jsonData;
	}
	
	public static JsonData from(String string) {
		try {
			return new JacksonJsonData(string, mapper.readTree(string));
		} catch (JsonProcessingException e) {
			throw new JsonDataException("Error while parsing JsonData string.", e);
		}
	}
	
	/**
	 * Determines the name of the root object
	 * 
	 * throws an exception of the root is an array.
	 * 
	 * @return
	 */
	@Override
	public String determineRootName() {
		List<String> topLevelFieldNames = listChildren();
		if (topLevelFieldNames.size() != 1) {
			// Should only be one root object.
			throw new IllegalStateException("Expected just one json root but found nodes %s".formatted(topLevelFieldNames.toString()));
		}
		return topLevelFieldNames.get(0);
	}

	
	/**
	 * Lists the names of the children of this object..
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public List<String> listChildren() {
		return listChildNames(rootNode);
	}	

	/**
	 * Lists the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public List<String> listChildrenAt(String jsonPtr) {
		return listChildrenAt(JacksonJsonDataPointer.of(jsonPtr));
	}	

	/**
	 * Lists the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public List<String> listChildrenAt(JsonDataPointer jsonDataPtr) {
		JsonNode node = rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer);
		return listChildNames(node);
	}	

	private List<String> listChildNames(JsonNode targetNode) {
		return childNames(targetNode).toList();
	}

	/**
	 * Streams the names of the children of this object..
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Stream<String> children() {
		return childNames(rootNode);
	}	

	/**
	 * Streams the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Stream<String> childrenAt(String jsonPtr) {
		return childrenAt(JacksonJsonDataPointer.of(jsonPtr));
	}	

	/**
	 * Streams the names of the children at this location.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Stream<String> childrenAt(JsonDataPointer jsonDataPtr) {
		JsonNode node = rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer);
		return childNames(node);
	}	

	private Stream<String> childNames(JsonNode targetNode) {
		return iteratorToStream(targetNode::fieldNames);
	}

	private <T> Stream<T> iteratorToStream(Supplier<Iterator<T>> iterator) {
		return StreamSupport.stream(((Iterable<T>)()->(iterator.get())).spliterator(), false);
	}

	/**
	 * Returns the value of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonDataPtr
	 * @return
	 */
	@Override
	public Optional<String> at(JsonDataPointer jsonDataPtr) {
		JsonNode node = rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer);
		return node.isValueNode() ? Optional.of(node.asText()) : Optional.empty();
	}
	
	/**
	 * Returns the value of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Optional<String> at(String jsonPtr) {
		return at(JacksonJsonDataPointer.of(jsonPtr));
	}
	
	/**
	 * Returns the JsonData of the JsonNode pointed at using a JsonPointer string.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Optional<JsonData> subsetAt(String jsonPtr) {
		return subsetAt(JacksonJsonDataPointer.of(jsonPtr));
	}
	
	/**
	 * Returns the JsonData of the JsonNode pointed at using a JsonPointer.
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public Optional<JsonData> subsetAt(JsonDataPointer jsonDataPtr) {
		JsonNode node = rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer);
		return node.isContainerNode() ? Optional.of(JacksonJsonData.from(node.toPrettyString())) : Optional.empty();
	}
	
	/**
	 * Returns a Stream of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonPtr - String containing JsonPointer
	 * @return
	 */
	@Override
	public Stream<JsonData> arrayAt(String jsonPtr) {
		return arrayAt(JacksonJsonDataPointer.of(jsonPtr));
	}

	/**
	 * Returns a Stream of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonDataPtr - JsonPointer
	 * @return
	 */
	@Override
	public Stream<JsonData> arrayAt(JsonDataPointer jsonDataPtr) {
		JsonNode node = rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer);
		if (!node.isArray()) { return Stream.empty(); }
		// We have an array, convert it to a list of JsonData
		Iterable<JsonNode> iterable = () -> node.elements();
		return StreamSupport.stream(iterable.spliterator(), false)	// get stream of JsonNodes
							.map(JsonNode::toPrettyString)			// convert to Strings
							.map(JacksonJsonData::from);					// convert back to JsonData
	}
	
	/**
	 * Returns a List of JsonData of the Array pointed at using a JsonPointer String
	 * 
	 * @param jsonPtr
	 * @return
	 */
	@Override
	public List<JsonData> listAt(String jsonPtr) {
		return arrayAt(jsonPtr).toList();
	}	

	/**
	 * Returns a List of JsonData of the Array pointed at using a JsonPointer
	 * 
	 * @param jsonDataPtr
	 * @return
	 */
	@Override
	public List<JsonData> listAt(JsonDataPointer jsonDataPtr) {
		return arrayAt(jsonDataPtr).toList();
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
	@Override
	public boolean hasNode(String jsonPtr) {
		return hasNode(JacksonJsonDataPointer.of(jsonPtr));
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
	@Override
	public boolean hasNode(JsonDataPointer jsonDataPtr) {
		return !rootNode.at(((JacksonJsonDataPointer)jsonDataPtr).jsonPointer).isMissingNode();
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
	@Override
	public JsonData insertJsonProperty(String jsonPointer, String property, JsonData value) {
		return insertJsonProperty(JacksonJsonDataPointer.of(jsonPointer), property, value);
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
	@Override
	public JsonData insertJsonProperty(String jsonPointer, String property, String value) {
		return insertJsonProperty(JacksonJsonDataPointer.of(jsonPointer), property, value);
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
	@Override
	public JsonData insertJsonProperty(JsonDataPointer jsonDataPointer, String property, JsonData value) {
		return insert(((JacksonJsonDataPointer)jsonDataPointer).jsonPointer, n->n.set(property, ((JacksonJsonData)value).rootNode));
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
	@Override
	public JsonData insertJsonProperty(JsonDataPointer jsonDataPointer, String property, String value) {
		
		return insert(((JacksonJsonDataPointer)jsonDataPointer).jsonPointer, n->n.put(property, value));
	}	

	// Internal routine that locates and inserts something to into the JsonData (returning a copy)
	// The op is a Consumer that takes an ObjectNode and does something to it to insert the new thing.
	private JsonData insert(JsonPointer insertionPoint, Consumer<ObjectNode> op) {
		JsonNode deepCopy = rootNode.deepCopy();
		JsonNode insertionNode = deepCopy.at(insertionPoint);
		if (!insertionNode.isObject()) {
			throw new JsonDataException("Insertion only allowed in existing objects.  Pointer='" + insertionPoint.toString() + "'.");
		}
		op.accept((ObjectNode)insertionNode);
		try {
			return JacksonJsonData.from(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(deepCopy));
		} catch (JsonProcessingException e) {
			throw new JsonDataException("Error whiole inserting into Json.", e);
		}
	}

	@Override
	public JsonDataPointer pointerOf(String pointerString) {
		return JacksonJsonData.pointer(pointerString);
	}

	public static JsonDataPointer pointer(String pointerString) {
		return JacksonJsonDataPointer.of(pointerString);
	}
	
	private static class JacksonJsonDataPointer implements JsonDataPointer {
		private final JsonPointer jsonPointer;

		private JacksonJsonDataPointer(JsonPointer jsonPointer) {
			this.jsonPointer = jsonPointer;
		}

		private static JsonDataPointer of(String pointerString) {
			return new JacksonJsonDataPointer(JsonPointer.valueOf(pointerString));
		}

		@Override
		public String toString() {
			return jsonPointer.toString();
		}
	}
	

}