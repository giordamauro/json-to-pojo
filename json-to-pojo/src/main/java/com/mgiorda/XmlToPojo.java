package com.mgiorda;

import java.io.File;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class XmlToPojo {

	public static void fromFile(String xmlFileName, String classPackage) {

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(xmlFileName);

		try {

			Document document = (Document) builder.build(xmlFile);
			Element root = document.getRootElement();

			generateCode(root, classPackage);
		} catch (Exception e) {

			throw new IllegalStateException(e);
		}
	}

	private static void generateCode(Element root, String classPackage) {

		JsonElement jsonElement = getJsonElement(root);
		JsonToPojo.generateCode(jsonElement, classPackage);
	}

	public static void fromXml(String xml, String classPackage) {

		SAXBuilder builder = new SAXBuilder();

		try {

			Document document = (Document) builder.build(xml);
			Element root = document.getRootElement();

			generateCode(root, classPackage);

		} catch (Exception e) {

			throw new IllegalStateException(e);
		}
	}

	private static JsonElement getJsonElement(Element element) {

		JsonElement jsonElement = null;

		if (isNull(element)) {
			jsonElement = JsonNull.INSTANCE;
		} else if (isPrimitive(element)) {
			jsonElement = getJsonPrimitive(element.getValue());
		} else if (isList(element)) {
			jsonElement = getJsonArray(element);
		} else {
			jsonElement = getJsonObject(element);
		}
		return jsonElement;
	}

	private static boolean isNull(Element element) {

		boolean isNull = false;

		if (isPrimitive(element)) {
			String value = element.getValue();
			if (value.equals("")) {
				isNull = true;
			}
		}

		return isNull;
	}

	private static boolean isPrimitive(Element element) {

		boolean isPrimitive = true;

		if (element.getAttributes().size() != 0) {
			isPrimitive = false;
		}

		@SuppressWarnings("unchecked")
		List<Element> children = element.getChildren();
		if (children.size() != 0) {
			isPrimitive = false;
		}

		return isPrimitive;
	}

	private static boolean isList(Element element) {

		boolean isList = true;

		if (element.getAttributes().size() == 0 && element.getChildren().size() > 0) {

			@SuppressWarnings("unchecked")
			List<Element> children = element.getChildren();

			String childNames = children.get(0).getName();

			for (int i = 1; i < children.size() && isList; i++) {

				if (!children.get(i).getName().equals(childNames)) {
					isList = false;
				}
			}
		} else {
			isList = false;
		}

		return isList;
	}

	private static JsonObject getJsonObject(Element obj) {

		JsonObject jsonObject = new JsonObject();

		@SuppressWarnings("unchecked")
		List<Attribute> attributes = obj.getAttributes();

		for (Attribute attribute : attributes) {

			JsonElement jsonValue = getJsonPrimitive(attribute.getValue());
			jsonObject.add(attribute.getName(), jsonValue);
		}

		@SuppressWarnings("unchecked")
		List<Element> children = obj.getChildren();

		for (Element child : children) {

			String fieldName = child.getName();

			JsonElement fieldElement = getJsonElement(child);
			jsonObject.add(fieldName, fieldElement);
		}

		return jsonObject;
	}

	private static JsonArray getJsonArray(Element array) {

		@SuppressWarnings("unchecked")
		List<Element> children = (List<Element>) array.getChildren();
		Element firstChild = children.get(0);

		JsonElement listElement = getJsonElement(firstChild);

		JsonArray jsonArray = new JsonArray();
		jsonArray.add(listElement);

		return jsonArray;
	}

	private static JsonPrimitive getJsonPrimitive(String value) {

		JsonPrimitive primitive = null;

		if (value.equals("true") || value.equals("false")) {

			primitive = new JsonPrimitive(Boolean.valueOf(value));

		} else {
			try {
				Double doubleValue = Double.valueOf(value);

				if (doubleValue != Math.round(doubleValue)) {
					primitive = new JsonPrimitive(doubleValue);
				} else {
					Long longValue = doubleValue.longValue();
					if (longValue >= Integer.MAX_VALUE) {
						primitive = new JsonPrimitive(longValue);
					} else {
						Integer intValue = longValue.intValue();
						primitive = new JsonPrimitive(intValue);
					}
				}
			} catch (Exception ex) {
				primitive = new JsonPrimitive(value);
			}
		}

		return primitive;
	}
}
