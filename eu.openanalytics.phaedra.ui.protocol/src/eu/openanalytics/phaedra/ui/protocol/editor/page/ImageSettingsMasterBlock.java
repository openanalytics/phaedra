package eu.openanalytics.phaedra.ui.protocol.editor.page;

import java.util.Iterator;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;


public class ImageSettingsMasterBlock extends MasterDetailsBlock {

	private TableViewer tableViewer;
	private WritableList inputList;

	private ImageSettingsPage parentPage;

	public ImageSettingsMasterBlock(WritableList input, ImageSettingsPage page) {
		this.parentPage = page;
		this.inputList = input;
	}

	@Override
	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();

		Composite channelsComposite = new Composite(parent, SWT.NONE);
		channelsComposite.setLayout(new GridLayout());
		toolkit.adapt(channelsComposite);

		Section channelsSection = toolkit.createSection(channelsComposite, Section.TITLE_BAR | Section.EXPANDED);
		channelsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		channelsSection.setText("Channels");

		Composite composite = toolkit.createComposite(channelsSection, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		toolkit.paintBordersFor(composite);
		channelsSection.setClient(composite);

		tableViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		Table table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		tableViewer.setContentProvider(new ArrayContentProvider());

		// Allow reordering via drag and drop.
		if (parentPage.getEditor().isSaveAsAllowed()) {
			int ops = DND.DROP_MOVE;
			Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer()};
			tableViewer.addDragSupport(ops, transfers, new DragSourceAdapter(){
				@Override
				public void dragStart(DragSourceEvent event) {
					ISelection sel = tableViewer.getSelection();
					LocalSelectionTransfer.getTransfer().setSelection(sel);
				}
			});
			tableViewer.addDropSupport(ops, transfers, new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
					ImageChannel channel = SelectionUtils.getFirstObject(sel, ImageChannel.class);
					
					int targetPos = -1;
					if (event.item != null) targetPos = getIndex((ImageChannel)event.item.getData());
					inputList.remove(getIndex(channel));
					if (targetPos == -1 || targetPos >= inputList.size()) inputList.add(channel);
					else inputList.add(targetPos, channel);

					refreshChannelOrder();
				}
			});
		}
		
		TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Name");
		tvc.getColumn().setWidth(200);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ImageChannel channel = (ImageChannel) element;
				return channel.getName();
			}
		});

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Type");
		tvc.getColumn().setWidth(150);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ImageChannel channel = (ImageChannel) element;
				IComponentType type = ComponentTypeFactory.getInstance().getComponent(channel);
				if (type != null) {
					return type.getName();
				}
				return "";
			}
		});

		tableViewer.setInput(inputList);

		final SectionPart part = new SectionPart(channelsSection);
		managedForm.addPart(part);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (event != null) {
					managedForm.fireSelectionChanged(part, event.getSelection());
					SWTUtils.smartRefresh(tableViewer, false);
				}
			}
		});

		final Composite buttonbar = toolkit.createComposite(channelsSection, SWT.TRANSPARENT);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		buttonbar.setLayout(gridLayout_1);
		channelsSection.setTextClient(buttonbar);
		toolkit.paintBordersFor(buttonbar);

		ImageHyperlink addChannelButton = toolkit.createImageHyperlink(buttonbar, SWT.TRANSPARENT);
		addChannelButton.setText("Add channel");
		addChannelButton.setImage(IconManager.getIconImage("channel_add.png"));
		addChannelButton.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(final HyperlinkEvent e) {
				addChannel();
			}
		});
		addChannelButton.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		
		ImageHyperlink deleteChannelButton = toolkit.createImageHyperlink(buttonbar, SWT.TRANSPARENT);
		deleteChannelButton.setText("Delete channel");
		deleteChannelButton.setImage(IconManager.getIconImage("channel_delete.png"));
		deleteChannelButton.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(final HyperlinkEvent e) {
				deleteChannel();
			}
		});
		deleteChannelButton.setEnabled(parentPage.getEditor().isSaveAsAllowed());
	}

	protected void deleteChannel() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		boolean yes = MessageDialog.openQuestion(shell, "Delete?",
				"Are you sure you want to delete the selected channel(s)?");
		if (!yes) return;

		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				ImageChannel channel = (ImageChannel) iterator.next();
				int index = getIndex(channel);
				if (index != -1) inputList.remove(index);
			}
			refreshChannelOrder();
		}
	}

	protected void addChannel() {
		ImageSettings settings = parentPage.getImageSettings();
		ImageChannel channel = ProtocolService.getInstance().createChannel(settings);
		inputList.add(channel);
		refreshChannelOrder();
	}

	private int getIndex(ImageChannel channel) {
		for (int i = 0; i < inputList.size(); i++) {
			if (channel == inputList.get(i)) return i;
		}
		return -1;
	}
	
	private void refreshChannelOrder() {
		for (int i = 0; i < inputList.size(); i++) {
			ImageChannel channel = (ImageChannel) inputList.get(i);
			channel.setSequence(i);
		}
		SWTUtils.smartRefresh(tableViewer, false);
		parentPage.markEditorDirty();
	}
	
	@Override
	protected void registerPages(DetailsPart part) {
		detailsPart.registerPage(ImageChannel.class, new ImageSettingsDetailBlock(parentPage, this));
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();

		Action haction = new Action("hor", Action.AS_RADIO_BUTTON) {
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText("Horizontal orientation");
		haction.setImageDescriptor(IconManager
				.getIconDescriptor("application_tile_horizontal.png"));

		Action vaction = new Action("ver", Action.AS_RADIO_BUTTON) {
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText("Vertical orientation");
		vaction.setImageDescriptor(IconManager
				.getIconDescriptor("application_split.png"));

		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}
	
	public void refreshViewer() {
		SWTUtils.smartRefresh(tableViewer, true);
	}
}
