package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.misc.DateUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient.ConnectionInfo;

public abstract class BaseListOperation<E> extends BaseOperation {

	private List<E> list;
	
	@Override
	public void execute(HttpClient client, ConnectionInfo connectionInfo) throws IOException {
		Document doc = XmlUtils.createEmptyDoc(true); 
		Element functionTag = createFunctionTag(doc, getOperationName());
		addCData(doc, functionTag, connectionInfo);
		
		String[] params = getOperationParameters();
		if (params != null) addKeyValueTags(doc, functionTag, params);
				
		String requestBody = XmlUtils.writeToString(doc);
		doc = doPostForXML(requestBody, client, connectionInfo);
		
		try {
			list = new ArrayList<>();
			NodeList matchingTags = XmlUtils.findTags(getMatchXPath(), doc);
			for (int i=0; i<matchingTags.getLength(); i++) {
				Element matchingTag = (Element)matchingTags.item(i);
				
				E newInstance = getObjectClass().newInstance();
				Field[] fields = getObjectClass().getFields();
				for (Field field: fields) {
					Element fieldTag = XmlUtils.getFirstElement(field.getName(), matchingTag);
					if (fieldTag == null) continue;
					String fieldValue = fieldTag.getTextContent();
					setFieldValue(newInstance, field, fieldValue);
				}
				list.add(newInstance);
			}
		} catch (IllegalAccessException | InstantiationException e) {
			throw new IOException("Failed to unwrap response value", e);
		}
	}
	
	public List<E> getList() {
		return list;
	}
	
	protected abstract Class<? extends E> getObjectClass();
	
	protected String[] getOperationParameters() {
		// Default: no parameters
		return null;
	}
	
	protected String getOperationName() {
		return "get" + getObjectName() + "List";
	}
	
	protected String getMatchXPath() {
		return "/Envelope/Body/" + getOperationName() + "Response/return";
	}
	
	private String getObjectName() {
		String className = this.getClass().getSimpleName();
		String objectName = className.substring(3, className.length()-1);
		return objectName;
	}
	
	private void setFieldValue(E newInstance, Field field, String stringValue) throws IllegalArgumentException, IllegalAccessException {
		Class<?> type = field.getType();
		if (type == String.class) {
			field.set(newInstance, stringValue);
		} else if (type == float.class) {
			field.set(newInstance, Float.parseFloat(stringValue));
		} else if (type == double.class) {
			field.set(newInstance, Double.parseDouble(stringValue));
		} else if (type == int.class) {
			field.set(newInstance, Integer.parseInt(stringValue));
		} else if (type == long.class) {
			field.set(newInstance, Long.parseLong(stringValue));
		} else if (type == short.class) {
			field.set(newInstance, Short.parseShort(stringValue));
		} else if (type == Date.class) {
			field.set(newInstance, DateUtils.parseDate(stringValue, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
		}
	}
}
