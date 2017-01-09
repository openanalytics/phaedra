package eu.openanalytics.phaedra.ui.link.importer.navigator;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;

public class ReadingProvider implements IElementProvider {

	private static final Format READING_DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm");
	
	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			IElement[] elements = new IElement[1];
			elements[0] = new Group("New Readings", "new.readings", null);
			return elements;
		} else if (parent.getId().equals("new.readings")) {
			Map<String,Integer> protocols = getProtocolsWithNewReadings();
			List<String> protocolIds = new ArrayList<>(protocols.keySet());
			Collections.sort(protocolIds);

			IElement[] elements = new IElement[protocols.size()];
			for (int i=0; i<elements.length; i++) {
				elements[i] = new Group(protocolIds.get(i), "new.readings.protocol." + protocolIds.get(i), parent.getId());
				String count = "(" + protocols.get(protocolIds.get(i)).toString() + ")";
				((Element)elements[i]).setDecorations(new String[]{count});
			}
			return elements;
		} else if (parent.getId().startsWith("new.readings.protocol.")) {
			List<PlateReading> readings = getUnlinkedReadings(parent.getId());

			IElement[] elements = new IElement[readings.size()];
			for (int i=0; i<elements.length; i++) {
				PlateReading r = readings.get(i);
				elements[i] = new Element(r.getBarcode(), "new.readings.reading." + r.getId(), parent.getId(), IconManager.getIconDescriptor("plate.png"));
				((Element)elements[i]).setData(r);
				String date = "(" + READING_DATE_FORMAT.format(r.getDate()) + ")";
				((Element)elements[i]).setDecorations(new String[]{null, date});
			}
			return elements;
		}
		return null;
	}

	private Map<String,Integer> getProtocolsWithNewReadings() {
		List<PlateReading> unlinkedReadings = DataCaptureService.getInstance().getUnlinkedReadings();
		Map<String,Integer> protocols = new HashMap<>();
		for (PlateReading reading: unlinkedReadings) {
			if (reading.getProtocol() != null) {
				Integer i = protocols.get(reading.getProtocol());
				if (i == null) i = 0;
				i++;
				protocols.put(reading.getProtocol(), i);
			}
		}
		return protocols;
	}

	private List<PlateReading> getUnlinkedReadings(String parentId) {
		String protocol = parentId.substring("new.readings.protocol.".length());
		List<PlateReading> readings = DataCaptureService.getInstance().getUnlinkedReadings(protocol);
		Collections.sort(readings, CaptureUtils.READING_DATE_SORTER);
		Collections.reverse(readings);
		return readings;
	}
}
