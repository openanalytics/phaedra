package eu.openanalytics.phaedra.ui.protocol.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.upload.UploadSystemManager;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ProtocolTableColumns {

	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		Function<Object, Boolean> editableChecker = (p) -> SecurityService.getInstance().check(Permissions.PROTOCOL_EDIT, p);
		Consumer<Object> saver = (p) -> ProtocolService.getInstance().updateProtocol((Protocol) p);
		
		config = ColumnConfigFactory.create("Protocol Id", "getId", DataType.Integer, 70);
		configs.add(config);

		config = ColumnConfigFactory.create("Protocol Name", "getName", DataType.String, 250);
		config.setEditingConfig(ColumnEditingFactory.create("getName", "setName", saver, editableChecker));
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", DataType.String, 200);
		config.setEditingConfig(ColumnEditingFactory.create("getDescription", "setDescription", saver, editableChecker));
		configs.add(config);

		config = ColumnConfigFactory.create("Team", DataType.String, 150);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Protocol p = (Protocol)element;
				return StringUtils.createSeparatedString(p.getOwners(), ",");
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSortComparator(new Comparator<Protocol>(){
			@Override
			public int compare(Protocol p1, Protocol p2) {
				if (p1 == null) return -1;
				if (p2 == null) return 1;
				String s1 = StringUtils.createSeparatedString(p1.getOwners(), ",");
				String s2 = StringUtils.createSeparatedString(p2.getOwners(), ",");
				return s1.compareTo(s2);
			}
		});
		config.setTooltip("Protocol Owner(s)");
		configs.add(config);

		config = ColumnConfigFactory.create("Upload System", "getUploadSystem", DataType.String, 100);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				return ((Protocol)element).getUploadSystem();
			}
			@Override
			public Image getImage(Object element) {
				Protocol p = (Protocol)element;
				if (p.getUploadSystem() != null) return UploadSystemManager.getInstance().getIcon(p.getUploadSystem());
				else return null;
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

}
