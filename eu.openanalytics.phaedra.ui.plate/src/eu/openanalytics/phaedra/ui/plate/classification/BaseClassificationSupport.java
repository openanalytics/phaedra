package eu.openanalytics.phaedra.ui.plate.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.ClassificationProvider;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationUtils;

/**
 * Base class for adding classification support to other components.
 *
 * This class can be used in two ways:
 * <p>
 * 1.
 * <ul>
 * <li>Pass data via the ISelectionListener interface</li>
 * <li>Create a toolbar or context menu via createToolbarButton or createContextMenuItem</li>
 * <li>Use the toolbar or context menu to open a ClassificationDialog and save the classification</li>
 * </ul>
 * </p>
 * <p>
 * 2.
 * <ul>
 * <li>Pass data via getCurrentBatch() and addBatch()</li>
 * <li>Save the classification via save()</li>
 * </ul>
 * </p>
 */
public abstract class BaseClassificationSupport<T> extends PartDecorator implements ISelectionListener {

	private final static int DIALOG_CONFIRM = 0;
	private final static int DIALOG_CONFIRM_DONT_ASK_AGAIN = 1;
	private final static int DIALOG_CANCEL = 2;

	private static ClassificationDialog<?> currentDialog;
	private boolean askBeforeSave = true;

	private List<ClassificationBatch<T>> classificationBatches = new ArrayList<>();
	private ClassificationBatch<T> currentBatch = new ClassificationBatch<>();

	private ProtocolClass currentProtocolClass;
	private ClassificationProvider currentClassificationProvider;

	private boolean showToolbar;
	private boolean showMenu;

	public BaseClassificationSupport() {
		this(true, true);
	}

	public BaseClassificationSupport(boolean showToolbar, boolean showMenu) {
		this.showToolbar = showToolbar;
		this.showMenu = showMenu;
	}

	@Override
	public void onCreate(Composite parent) {
		super.onCreate(parent);
		getWorkBenchPart().getSite().getPage().addSelectionListener(this);

		SelectionUtils.triggerActiveEditorSelection(this);
	}

	@Override
	public void onDispose() {
		super.onDispose();
		getWorkBenchPart().getSite().getPage().removeSelectionListener(this);
	}

	@Override
	public void contributeContextMenu(IMenuManager manager) {
		super.contributeContextMenu(manager);
		if (showMenu) createContextMenuItem(manager);
	}

	@Override
	public void contributeToolbar(IToolBarManager manager) {
		super.contributeToolbar(manager);
		if (showToolbar) {
			ContributionItem contributionItem = new ContributionItem() {
				@Override
				public void fill(ToolBar parent, int index) {
					createToolbarButton(parent);
				}
			};
			manager.add(contributionItem);
		}
	}

	public void createToolbarButton(ToolBar parent) {
		ToolItem classificationSelectionButton = new ToolItem(parent, SWT.PUSH);
		classificationSelectionButton.setImage(IconManager.getIconImage("tag_blue_edit.png"));
		classificationSelectionButton.setToolTipText("Edit classification");
		classificationSelectionButton.addListener(SWT.Selection, e -> openDialog());
	}

	public void createContextMenuItem(IMenuManager manager) {
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
				menuItem.setImage(IconManager.getIconImage("tag_blue_edit.png"));
				menuItem.setText("Edit Classification");
				menuItem.addListener(SWT.Selection, e -> openDialog());
			}
		};
		manager.add(contributionItem);
	}

	public ClassificationBatch<T> getCurrentBatch() {
		return currentBatch;
	}

	public void addBatch() {

		// Abort if there is nothing to classify.
		if (currentBatch.getItems() == null || currentBatch.getItems().length == 0) return;

		String msg = null;
		if (currentBatch == null) msg = "Cannot add null batch";
		if (currentBatch.getFeatureClass() == null) msg = "Cannot add classification: no feature class selected";
		if (msg != null) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Cannot add classification", msg);
			return;
		}

		classificationBatches.add(currentBatch);

		if (Display.getCurrent() != null) {
			msg = "" + currentBatch.getItems().length + " items will be classified as " + currentBatch.getFeatureClass().getLabel()
					+ ".\nYou can select other items to classify, or click Finish now to save the classification change(s).";
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Classification added", msg);
		}

		currentBatch = new ClassificationBatch<>();
	}

	public void save(IProgressMonitor monitor) throws IOException {
		// Make a copy of the list to prevent concurrent modification.
		final List<ClassificationBatch<T>> batchesToSave = new ArrayList<>(classificationBatches);
		doSave(batchesToSave, monitor);
	}

	public int getNumBatches() {
		return classificationBatches.size();
	}

	/*
	 * Non-public
	 * **********
	 */

	private void openDialog() {
		if (currentDialog != null) return;

		ClassificationProvider[] providers = getClassificationProviders();
		if (providers.length > 0) {
			if (currentClassificationProvider == null) currentClassificationProvider = providers[0];
		} else {
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), "No classification enabled",
					"Cannot classify: the protocol class does not contain any classification features.");
			return;
		}

		// Discard any previous unsaved batches.
		classificationBatches.clear();

		try {
			currentDialog = new ClassificationDialog<T>(Display.getDefault().getActiveShell(), this);
			currentDialog.open();
		} finally {
			currentDialog = null;
		}
	}

	protected void refreshDialog() {
		if (currentDialog != null) currentDialog.reloadItemTable();
	}

	protected String formatCount(int count, int totalCount, boolean asPercentage) {
		if (asPercentage) {
			return String.format("%.0f%%", (((double)count/totalCount)*100));
		} else {
			return count+"";
		}
	}

	protected void checkCanModify(Plate plate) {
		ValidationUtils.checkCanModifyPlate(plate);
	}

	protected boolean doSave() {

		if (classificationBatches.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot save classification", "Select at least one item to classify.");
			return false;
		}

		Set<Plate> affectedPlates = new HashSet<>();
		for (ClassificationBatch<T> batch: classificationBatches) affectedPlates.addAll(getAffectedPlates(batch));
		for (Plate plate: affectedPlates) {
			if (ValidationService.getInstance().getPlateStatus(plate).validationSet) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot save",
						"Cannot save classification: " + plate + " is already (in)validated.\n"
						+ "Please reset validation status before saving the classification.");
				return false;
			}
		}
		
		// Ask for confirmation before proceeding.
		int confirmed = DIALOG_CONFIRM;
		if (askBeforeSave) {
			MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
					"Confirm change", null,
					"Are you sure you want to update the classification for the selected items?",
					MessageDialog.QUESTION, new String[]{"Yes", "Yes, do not ask again", "Cancel"}, 0);
			confirmed = md.open();
			if (confirmed == DIALOG_CONFIRM_DONT_ASK_AGAIN) askBeforeSave = false;
		}
		if (confirmed == DIALOG_CANCEL) return false;

		// Make a copy of the list to prevent concurrent modification.
		final List<ClassificationBatch<T>> batchesToSave = new ArrayList<>(classificationBatches);

		Job job = new Job("Saving classification data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					doSave(batchesToSave, monitor);
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to save classification", e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule(500);

		// Reason for delay: when the dialog closes, the underlying canvas is redrawn,
		// which in turn may trigger a read of the HDF5 file. If the job also opens the HDF5 file
		// at the same time, one of them may fail (due to the JHDF5 syncing mechanism).

		return true;
	}

	protected ProtocolClass getCurrentProtocolClass() {
		return currentProtocolClass;
	}

	protected void setCurrentProtocolClass(ProtocolClass currentProtocolClass) {
		this.currentProtocolClass = currentProtocolClass;
		setCurrentClassificationProvider(null);
	}

	protected ClassificationProvider getCurrentClassificationProvider() {
		return currentClassificationProvider;
	}

	protected void setCurrentClassificationProvider(ClassificationProvider provider) {
		this.currentClassificationProvider = provider;

		//TODO Is this reset sufficient?
		// Reset any previously added batches.
		classificationBatches.clear();
	}

	protected Set<Plate> getAffectedPlates(ClassificationBatch<T> batch) {
		Set<Plate> plates = new HashSet<>();
		for (T item: batch.getItems()) {
			Plate p = SelectionUtils.getAsClass(item, Plate.class);
			if (p != null) plates.add(p);
		}
		return plates;
	}
	
	protected abstract void doSave(List<ClassificationBatch<T>> batches, IProgressMonitor monitor) throws IOException;

	protected abstract String calculateCount(FeatureClass featureClass, boolean asPercentage);
	protected abstract ColumnConfiguration[] createItemTableColumns();
	protected abstract ClassificationProvider[] getClassificationProviders();
}
