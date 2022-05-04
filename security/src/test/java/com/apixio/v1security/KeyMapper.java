package com.apixio.v1security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

class KeyMapper {

	public static Map<String, String> map = new HashMap<String, String>();

	public KeyMapper() {
	}

	protected static Map<String, String> setKey(String keyXml) throws DocumentException {
		String version = "";
		String key = "";
		String isDefault = "";
		Element element = null;
		Element childElement = null;
		if (keyXml.startsWith("<")) {
			Document document = DocumentHelper.parseText(keyXml);
			Element root = document.getRootElement();
			for (Iterator i = root.elementIterator("key"); i.hasNext();) {
				element = (Element) i.next();
				List elements = element.elements();
				for (int j = 0; j < elements.size(); j++) {
					childElement = (Element) elements.get(j);
					if (childElement.getName().equalsIgnoreCase("version")) {
						version = childElement.getText();
					}
					if (childElement.getName().equalsIgnoreCase("key")) {
						key = childElement.getText();
					}
					if (childElement.getName().equalsIgnoreCase("is-default")) {
						isDefault = childElement.getText();
					}
				}

				map.put(version, key);
				if (isDefault.equalsIgnoreCase("Y")) {
					map.put("default", (new StringBuilder(String.valueOf(version))).append("_").append(key).toString());
				}
			}

		} else if (keyXml.equalsIgnoreCase("unauthorized")) {
			map = null;
			System.out.println("Source is not authorized ...");
		} else {
			map = null;
			System.out.println("KeyService not started ...");
		}
		return map;
	}

}
