package eu.openanalytics.phaedra.ui.plate.inspector.compound;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.beans.MViewPane;
import chemaxon.struc.Molecule;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfo;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;

public class CompoundInspector extends DecoratedView implements ISelectionListener {

	private BreadcrumbViewer breadcrumb;
	private String noCompoundMessage;

	private SplitComposite splitComp;
	private MViewPanePaint viewPane;
	private TableViewer infoTableViewer;

	private Compound currentCompound = null;
	private Molecule molecule = null;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		SplitCompositeFactory.getInstance().prepare(memento, SplitComposite.MODE_V_1_2);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		splitComp.save(memento);
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().spacing(0,0).margins(0, 0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(3,3).applyTo(container);

		splitComp = SplitCompositeFactory.getInstance().create(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComp);

		noCompoundMessage = "";

		createSmilesPanel(splitComp);
		createTable(splitComp);

		splitComp.setWeights(new int[] { 50, 50 });

		getViewSite().getActionBars().getToolBarManager().add(splitComp.createModeButton());

		addDecorator(new SelectionHandlingDecorator(this));
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		getSite().getPage().addSelectionListener(this);
		selectionChanged(getSite().getPage().getActivePart(), getSite().getPage().getSelection());

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewCompoundInspector");
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	public void createSmilesPanel(Composite container) {
		Composite swtAwtComponent = new Composite(container, SWT.EMBEDDED | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(swtAwtComponent);

		swtAwtComponent.setLayout(new FillLayout());
		java.awt.Frame frame = SWT_AWT.new_Frame(swtAwtComponent);
		viewPane = new MViewPanePaint();
		viewPane.setParams("rows=1\ncols=1\nvisibleRows=1\nextrabonds=arom,wedge\n");
		viewPane.setBorderWidth(1);
		viewPane.setEditable(2);
		viewPane.setBackground(new Color(0xcccccc));
		viewPane.setMolbg(new Color(0xffffff));
		frame.add(viewPane);
	}

	public void createTable(Composite container) {

		infoTableViewer = new TableViewer(container, SWT.BORDER);
		Table table = infoTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT,200).grab(true,false).applyTo(table);

		infoTableViewer.setContentProvider(new ArrayContentProvider());
		infoTableViewer.setInput(new Object());

		final TableViewerColumn featureColumn = new TableViewerColumn(infoTableViewer, SWT.NONE);
		featureColumn.getColumn().setWidth(125);
		featureColumn.getColumn().setText("Property");
		featureColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "-";
				if (element instanceof String[]) {
					String[] entry = (String[]) element;
					if (entry.length >= 1) {
						String field = entry[0].replace("_", " ");
						return field;
					}
				}
				return "-";
			}
		});

		final TableViewerColumn valueColumn = new TableViewerColumn(infoTableViewer, SWT.NONE);
		valueColumn.getColumn().setWidth(300);
		valueColumn.getColumn().setText("Value");
		valueColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "-";
				if (element instanceof String[]) {
					String[] entry = (String[]) element;
					if (entry.length >= 2)
						return entry[1];
				}
				return "-";
			}
		});
	}

	@Override
	public void setFocus() {
		infoTableViewer.getTable().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part == this) return;
		Compound c = SelectionUtils.getFirstObject(selection, Compound.class);

		if (c == null) {
			// Support for Project compounds.
			Object o = SelectionUtils.getFirstObject(selection, Object.class);
			Object type = ReflectionUtils.invoke("getType", o);
			Object nr = ReflectionUtils.invoke("getNumber", o);
			if (type != null && nr != null) {
				List<Compound> compounds = PlateService.getInstance().getCompounds(type.toString(), nr.toString());
				if (!compounds.isEmpty()) c = compounds.get(0);
				else {
					c = new Compound();
					c.setType(type.toString());
					c.setNumber(nr.toString());
				}
			}
		}

		if (c != null) {
			setCompound(c);
		} else {
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			long sampleCount = wells.stream().filter(w -> w.getCompound() != null).count();
			if (!wells.isEmpty() && sampleCount == 0) {
				// Only controls/empties selected: clear chart.
				setCompound(null);
			}
		}
	}

	private void setCompound(Compound compound) {
		if (compound == null || compound.getNumber() == null) {
			breadcrumb.setInput(null);
			breadcrumb.getControl().getParent().layout();

			infoTableViewer.getTable().clearAll();
			noCompoundMessage = "No compound for this well";
			currentCompound = null;
			viewPane.setM(0, (Molecule)null);
			return;
		}
		if (currentCompound != null && compound.getNumber().equals(currentCompound.getNumber())) return;
		currentCompound = compound;
		
		noCompoundMessage = "Loading...";
		List<String[]> loading = new ArrayList<>();
		loading.add(new String[] { noCompoundMessage, "" });
		infoTableViewer.setInput(loading);

		breadcrumb.setInput(compound);
		breadcrumb.getControl().getParent().layout();

		JobUtils.runUserJob(monitor -> {
			CompoundInfo data = CompoundInfoService.getInstance().getInfo(compound);
			List<String[]> info = new ArrayList<String[]>();
			
			if (data != null) {
				for (String key: data.getKeys()) {
					String value = data.get(key);
					String[] row = new String[] { key, (value == null) ? "-" : value };
					info.add(row);
				}

				String smiles = data.getSmiles();
				try {
					molecule = MolImporter.importMol(smiles);
				} catch (Exception e) {}
			}

			Display.getDefault().asyncExec(() -> {
				noCompoundMessage = "";
				if (!infoTableViewer.getTable().isDisposed()) infoTableViewer.setInput(info);
				viewPane.setM(0, molecule);
			});
			
		}, "Loading Compound data (" + compound.toString() + ")", 100, toString(), null);
	}

	private class MViewPanePaint extends MViewPane {
		
		private static final long serialVersionUID = 1L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			// paint possible message
			if (!noCompoundMessage.isEmpty()) {
				g.setColor(Color.BLACK);
				FontMetrics metr = g.getFontMetrics();
				Rectangle2D rect = metr.getStringBounds(noCompoundMessage, g);
				g.drawString(noCompoundMessage, (int) ((this.getSize().width - rect.getWidth()) / 2), (int) ((this.getSize().height - rect.getHeight()) / 2));
			}
		}
	}
}