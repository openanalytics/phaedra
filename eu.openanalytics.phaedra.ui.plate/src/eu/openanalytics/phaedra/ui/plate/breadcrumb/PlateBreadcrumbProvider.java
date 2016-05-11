package eu.openanalytics.phaedra.ui.plate.breadcrumb;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseExperiments;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowsePlates;
import eu.openanalytics.phaedra.ui.plate.cmd.EditExperiment;
import eu.openanalytics.phaedra.ui.plate.cmd.EditPlate;
import eu.openanalytics.phaedra.ui.plate.cmd.ShowQuickHeatmap;
import eu.openanalytics.phaedra.ui.plate.inspector.experiment.ExperimentInspector;
import eu.openanalytics.phaedra.ui.plate.inspector.plate.PlateInspector;
import eu.openanalytics.phaedra.ui.plate.inspector.well.WellInspector;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.IBreadcrumbProvider;
import eu.openanalytics.phaedra.ui.protocol.cmd.BrowseProtocols;
import eu.openanalytics.phaedra.ui.protocol.cmd.EditProtocol;
import eu.openanalytics.phaedra.ui.protocol.cmd.EditProtocolClass;

public class PlateBreadcrumbProvider implements IBreadcrumbProvider {

	@Override
	public void addMenuContribution(final Object o, final Table table, final TreePath path) {
		if (o instanceof Experiment || o instanceof Plate || o instanceof Well) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setImage(0, IconManager.getIconImage("magnifier.png"));
			item.setText(1, "Inspect");
			item.setData(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					inspect(path.getLastSegment());
				}
			});
		}
		
		if (o instanceof ProtocolClass || o instanceof Protocol || o instanceof Experiment || o instanceof Plate) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setImage(0, IconManager.getIconImage("folder_open.png"));
			item.setText(1, "Open");
			item.setData(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					open(path.getLastSegment());
				}
			});
		}
		
		if (o instanceof ProtocolClass || o instanceof Protocol || o instanceof Experiment || o instanceof Plate) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setImage(0, IconManager.getIconImage("pencil.png"));
			item.setText(1, "Edit");
			item.setData(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					edit(path.getLastSegment());
				}
			});
		}
	}

	private static void inspect(Object o) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			if (o instanceof Experiment) page.showView(ExperimentInspector.class.getName());
			if (o instanceof Plate) page.showView(PlateInspector.class.getName());
			if (o instanceof Well) page.showView(WellInspector.class.getName());
		} catch (PartInitException e) {}
	}

	private static void open(Object o) {
		if (o instanceof ProtocolClass) BrowseProtocols.execute((ProtocolClass)o);
		if (o instanceof Protocol) BrowseExperiments.execute((Protocol)o);
		if (o instanceof Experiment) BrowsePlates.execute((Experiment)o);
		if (o instanceof Plate) ShowQuickHeatmap.execute((Plate)o);
	}
	
	private static void edit(Object o) {
		if (o instanceof ProtocolClass) EditProtocolClass.execute((ProtocolClass)o);
		if (o instanceof Protocol) EditProtocol.execute((Protocol)o);
		if (o instanceof Experiment) EditExperiment.execute((Experiment)o);
		if (o instanceof Plate) EditPlate.execute((Plate)o);
	}

}