package eu.openanalytics.phaedra.base.db.jpa;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;

/**
 * This custom ClassLoader creates a merged persistence.xml file taking
 * class names from the extension point persistenceModel.
 * It is meant to be used by a JPA PersistenceProvider to allow models
 * contributed by multple plugins to be merged into one persistence unit.
 * 
 * To resolve the entity classes, this plugin needs access to the classloaders
 * of the contributing plugins (see Eclipse-BuddyPolicy in manifest).
 */
public class PersistenceXMLClassLoader extends ClassLoader {

	public static final String MODEL_NAME = "datamodel";
	public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
	
	private URL persistenceURL;
	
	public PersistenceXMLClassLoader(ClassLoader delegate) {
		super(delegate);
	}
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if (name.equals(PERSISTENCE_XML)) {
			if (persistenceURL == null) {
				persistenceURL = getPersistenceXML();
			}
			return new Enumeration<URL>() {
				private boolean consumed = false;

				@Override
				public boolean hasMoreElements() {
					return !consumed;
				}
				@Override
				public URL nextElement() {
					consumed = true;
					return persistenceURL;
				}
			};
		} else {
			return super.getResources(name);
		}
	}
	
	private URL getPersistenceXML() throws IOException {
		
		final File file = new File(System.getProperty("java.io.tmpdir") + "/screening-core-jpa/" + PERSISTENCE_XML);
		file.getParentFile().mkdirs();
		
		String xml = createPersistenceXMLBody();
		StreamUtils.copyAndClose(new ByteArrayInputStream(xml.getBytes()), new FileOutputStream(file));
		
		return new URL("file://" + file.getAbsolutePath());
	}
	
	private String createPersistenceXMLBody() {
		String[] classNames = EntityClassManager.getRegisteredEntityClassNames();
		
		String lb = System.getProperty("line.separator");
		
		StringBuilder xmlBuilder = new StringBuilder();
		xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + lb);
		xmlBuilder.append("<persistence xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lb);
		xmlBuilder.append("	xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\"" + lb);
		xmlBuilder.append("	version=\"2.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\">" + lb);
		xmlBuilder.append("	<persistence-unit name=\"" + MODEL_NAME + "\">" + lb);
		for (String className: classNames) xmlBuilder.append("	<class>" + className + "</class>" + lb);
		xmlBuilder.append("	</persistence-unit>" + lb);
		xmlBuilder.append("</persistence>");
		return xmlBuilder.toString();
	}
}
