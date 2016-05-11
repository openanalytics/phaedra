package eu.openanalytics.phaedra.ui.protocol.editor.page;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;

public class ImageSettingsDetailBlock implements IDetailsPage {

	private ImageChannel channel;
	private ImageSettingsPage parentPage;
	private ImageSettingsMasterBlock masterBlock;
	private IManagedForm managedForm;
	private DataBindingContext m_bindingContext;
	
	private Text nameTxt;
	private Text descriptionTxt;
	
	private CCombo typeCmb;
	private CCombo bitCmb;
	
	private Label maxLabel;
	private Scale scaleMax;
	private Text maxTxt;
	
	private Label minLabel;
	private Scale scaleMin;
	private Text minTxt;
	
	private Label alphaLabel;
	private Label alphaValue;
	private Scale scaleAlpha;
	
	private Button checkWellView;
	private Button checkPlateView;

	private Composite colorMethodComposite;

	public ImageSettingsDetailBlock(ImageSettingsPage page, ImageSettingsMasterBlock masterBlock) {
		this.parentPage = page;
		this.masterBlock = masterBlock;
	}

	public void initialize(IManagedForm form) {
		managedForm = form;
	}

	public void createContents(final Composite parent) {
		
		FormToolkit toolkit = managedForm.getToolkit();
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(parent);

		Section section = toolkit.createSection(parent, ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setText("Channel section");

		Composite composite = toolkit.createComposite(section, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(composite);
		toolkit.paintBordersFor(composite);
		section.setClient(composite);

		/*
		 * General info
		 */
		
		Label lbl = toolkit.createLabel(composite, "Name:", SWT.NONE);
		GridDataFactory.fillDefaults().hint(90, SWT.DEFAULT).applyTo(lbl);

		nameTxt = toolkit.createText(composite, null, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameTxt);

		lbl = toolkit.createLabel(composite, "Description:", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

		descriptionTxt = toolkit.createText(composite, null, SWT.WRAP);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).hint(SWT.DEFAULT, 30).applyTo(descriptionTxt);

		toolkit.createLabel(composite, "Type:", SWT.NONE);

		Composite typeContainer = toolkit.createComposite(composite, SWT.NONE);
		// Done for typeCmb, else it does not show border properly. A more suitable fix is always welcome.
		GridLayoutFactory.fillDefaults().margins(1, 1).numColumns(2).applyTo(typeContainer);
		toolkit.paintBordersFor(typeContainer);
		
		IComponentType[] types = ComponentTypeFactory.getInstance().getKnownTypes();
		
		typeCmb = new CCombo(typeContainer, SWT.BORDER | SWT.READ_ONLY);
		typeCmb.setData(types);
		GridDataFactory.fillDefaults().hint(165, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(typeCmb);
		
		ComboViewer typeViewer = new ComboViewer(typeCmb);
		typeViewer.setContentProvider(new ArrayContentProvider());
		typeViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				IComponentType type = (IComponentType)element;
				return type.getName();
			}
		});
		typeViewer.setInput(types);
		toolkit.adapt(typeCmb, true, true);
		
		Link link = new Link(typeContainer, SWT.NONE);
		link.setText("<a>More info</a>");
		final DefaultToolTip tt = new DefaultToolTip(link, SWT.NONE, true);
		link.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				IComponentType type = getCurrentComponentType();
				tt.setText(type.getName() + ":\n" + type.getDescription());
				tt.show(new Point(event.x, event.y));
			}
		}); 

		toolkit.createLabel(composite, "Bit Depth:", SWT.NONE);
		
		bitCmb = new CCombo(composite, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().hint(165, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(bitCmb);

		ComboViewer bitCmbViewer = new ComboViewer(bitCmb);
		bitCmbViewer.setContentProvider(new ArrayContentProvider());
		bitCmbViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if ((Integer)element == ImageChannel.CHANNEL_BIT_DEPTH_1) return "1 Bit";
				if ((Integer)element == ImageChannel.CHANNEL_BIT_DEPTH_8) return "8 Bit";
				if ((Integer)element == ImageChannel.CHANNEL_BIT_DEPTH_16) return "16 Bit";
				return "";
			}
		});
		bitCmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int depth = SelectionUtils.getFirstObject(event.getSelection(), Integer.class);
				adjustMinMaxSliders(depth);
			}
		});
		bitCmbViewer.setInput(new Integer[]{ImageChannel.CHANNEL_BIT_DEPTH_1, ImageChannel.CHANNEL_BIT_DEPTH_8, ImageChannel.CHANNEL_BIT_DEPTH_16});
		toolkit.adapt(bitCmb, true, true);
		
		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).hint(SWT.DEFAULT, 20).applyTo(separator);
		toolkit.adapt(separator, true, true);

		toolkit.createLabel(composite, "Visible In:", SWT.NONE);

		checkPlateView = toolkit.createButton(composite, "Small views and thumbnails", SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(checkPlateView);
		new Label(composite, SWT.NONE);

		checkWellView = toolkit.createButton(composite, "Detailed views", SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(checkWellView);
		new Label(composite, SWT.NONE);

		separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).hint(SWT.DEFAULT, 20).applyTo(separator);
		toolkit.adapt(separator, true, true);

		toolkit.createLabel(composite, "Coloring:", SWT.NONE);

		colorMethodComposite = toolkit.createComposite(composite, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(colorMethodComposite);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(colorMethodComposite);
		toolkit.paintBordersFor(colorMethodComposite);
		
		separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).hint(SWT.DEFAULT, 20).applyTo(separator);
		toolkit.adapt(separator, true, true);

		alphaLabel = toolkit.createLabel(composite, "Alpha:", SWT.NONE);
		alphaLabel.setLayoutData(new GridData());

		Composite compositeAlpha = toolkit.createComposite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeAlpha);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(compositeAlpha);
		toolkit.paintBordersFor(compositeAlpha);

		toolkit.createLabel(compositeAlpha, "", SWT.NONE).setImage(IconManager.getIconImage("alpha.png"));

		scaleAlpha = new Scale(compositeAlpha, SWT.NONE);
		scaleAlpha.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				alphaValue.setText(String.valueOf(scaleAlpha.getSelection()));
			}
		});
		scaleAlpha.setSelection(255);
		scaleAlpha.setMaximum(255);
		toolkit.adapt(scaleAlpha, true, true);

		alphaValue = toolkit.createLabel(compositeAlpha, "", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,true).applyTo(alphaValue);

		minLabel = toolkit.createLabel(composite, "Min:", SWT.NONE);

		Composite compositeMin = toolkit.createComposite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeMin);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(compositeMin);
		toolkit.paintBordersFor(compositeMin);

		toolkit.createLabel(compositeMin, "", SWT.NONE).setImage(IconManager.getIconImage("contrast_low.png"));

		scaleMin = new Scale(compositeMin, SWT.NONE);
		scaleMin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				minTxt.setText("" + scaleMin.getSelection());
			}
		});
		final GridData gd_scaleMin = new GridData();
		scaleMin.setLayoutData(gd_scaleMin);
		scaleMin.setMaximum(255);
		toolkit.adapt(scaleMin, true, true);

		minTxt = toolkit.createText(compositeMin, "", SWT.NONE);
		minTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					int value = Integer.parseInt(minTxt.getText());
					value = Math.min(value, scaleMin.getMaximum());
					scaleMin.setSelection(value);
					channel.setLevelMin(value);
				} catch (NumberFormatException ex) {}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(50, SWT.DEFAULT).applyTo(minTxt);

		maxLabel = toolkit.createLabel(composite, "Max:", SWT.NONE);

		Composite compositeMax = toolkit.createComposite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeMax);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(compositeMax);
		toolkit.paintBordersFor(compositeMax);

		toolkit.createLabel(compositeMax, "", SWT.NONE).setImage(IconManager.getIconImage("contrast_high.png"));

		scaleMax = new Scale(compositeMax, SWT.NONE);
		scaleMax.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				maxTxt.setText("" + scaleMax.getSelection());
			}
		});
		scaleMax.setMaximum(255);
		toolkit.adapt(scaleMax, true, true);

		maxTxt = toolkit.createText(compositeMax, "", SWT.NONE);
		maxTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					int value = Integer.parseInt(maxTxt.getText());
					value = Math.min(value, scaleMax.getMaximum());
					scaleMax.setSelection(value);
					channel.setLevelMax(value);
				} catch (NumberFormatException ex) {}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(50, SWT.DEFAULT).applyTo(maxTxt);
		
		KeyAdapter dirtyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				parentPage.markEditorDirty();
				masterBlock.refreshViewer();
			}
		};

		SelectionAdapter selectionDirtyAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parentPage.markEditorDirty();
				masterBlock.refreshViewer();
			}
		};

		nameTxt.addKeyListener(dirtyAdapter);
		descriptionTxt.addKeyListener(dirtyAdapter);
		checkPlateView.addSelectionListener(selectionDirtyAdapter);
		checkWellView.addSelectionListener(selectionDirtyAdapter);
		minTxt.addKeyListener(dirtyAdapter);
		maxTxt.addKeyListener(dirtyAdapter);
		
		scaleAlpha.addSelectionListener(selectionDirtyAdapter);
		scaleMin.addSelectionListener(selectionDirtyAdapter);
		scaleMax.addSelectionListener(selectionDirtyAdapter);

		typeCmb.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setToggleMinMax();
				setToggleColorMethodControls();
				parentPage.markEditorDirty();
				masterBlock.refreshViewer();
			}
		});
		bitCmb.addSelectionListener(selectionDirtyAdapter);

		composite.setEnabled(parentPage.getEditor().isSaveAsAllowed());
	}

	public void dispose() {
		// Dispose
	}

	public void setFocus() {
		// Set focus
	}

	private void update() {
		// Update
	}

	public boolean setFormInput(Object input) {
		return false;
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		if (structuredSelection.isEmpty()) {
			return;
		}
		ImageChannel channel = (ImageChannel)structuredSelection.getFirstElement();

		if (m_bindingContext != null) {
			Object[] list = m_bindingContext.getBindings().toArray();
			for (Object object : list) {
				Binding binding = (Binding) object;
				m_bindingContext.removeBinding(binding);
				binding.dispose();
			}

			m_bindingContext.dispose();
		}

		this.channel = channel;
		adjustMinMaxSliders(channel.getBitDepth());
		m_bindingContext = initDataBindings();

		minTxt.setText(String.valueOf(channel.getLevelMin()));
		maxTxt.setText(String.valueOf(channel.getLevelMax()));
		alphaValue.setText(String.valueOf(channel.getAlpha()));

		setToggleMinMax();
		setToggleColorMethodControls();

		update();
	}

	public void commit(boolean onSave) {
		// Commit
	}

	public boolean isDirty() {
		return false;
	}

	public boolean isStale() {
		return false;
	}

	public void refresh() {
		update();
	}

	public void setToggleMinMax() {
		boolean on = (getCurrentComponentType().getId() == ImageChannel.CHANNEL_TYPE_RAW);

		scaleMin.setEnabled(on);
		scaleMax.setEnabled(on);
		maxTxt.setEnabled(on);
		minTxt.setEnabled(on);
		maxLabel.setEnabled(on);
		minLabel.setEnabled(on);
		
		scaleAlpha.setEnabled(!on);
		alphaValue.setEnabled(!on);
		alphaLabel.setEnabled(!on);
	}

	private void setToggleColorMethodControls() {
		// Remove current content, if any.
		for (Control c: colorMethodComposite.getChildren()) {
			c.dispose();
		}
		
		// Listen to changes made by the config area.
		ISelectionChangedListener changeListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ComponentTypeFactory.getInstance().purgeCache(channel);
				parentPage.markEditorDirty();
			}
		};
		
		// We are changing the type of the channel. Purge the cache.
		ComponentTypeFactory.getInstance().purgeCache(channel);
		
		IComponentType type = getCurrentComponentType();
		type.createConfigArea(colorMethodComposite, channel.getChannelConfig(), changeListener);
		colorMethodComposite.layout();
		colorMethodComposite.getParent().layout();
	}
	
	private void adjustMinMaxSliders(int depth) {
		int currentRange = scaleMin.getMaximum();
		int newRange = ImageUtils.getMaxColors(depth);
		if (currentRange == newRange) return;
		
		int currentMin = scaleMin.getSelection();
		int currentMax = scaleMax.getSelection();
		
		scaleMin.setMaximum(newRange);
		scaleMax.setMaximum(newRange);

		if (currentMin > newRange) {
			scaleMin.setSelection(newRange);
			minTxt.setText("" + newRange);
		}
		if (currentMax > newRange) {
			scaleMax.setSelection(newRange);
			maxTxt.setText("" + newRange);
		}
	}

	private IComponentType getCurrentComponentType() {
		IComponentType[] types = (IComponentType[])typeCmb.getData();
		IComponentType t = types[typeCmb.getSelectionIndex()];
		return t;
	}
	
	protected DataBindingContext initDataBindings() {
		DataBindingContext ctx = new DataBindingContext();
		FormEditorUtils.bindText(nameTxt, channel, "name", ctx);
		FormEditorUtils.bindText(descriptionTxt, channel, "description", ctx);
		FormEditorUtils.bindSelection(scaleAlpha, channel, "alpha", ctx);
		FormEditorUtils.bindSelection(scaleMin, channel, "levelMin", ctx);
		FormEditorUtils.bindSelection(scaleMax, channel, "levelMax", ctx);
		FormEditorUtils.bindSelection(checkPlateView, channel, "showInPlateView", ctx);
		FormEditorUtils.bindSelection(checkWellView, channel, "showInWellView", ctx);

		if (channel != null) {
			ChannelSourceMapper sourceMapper = new ChannelSourceMapper(channel);
//			FormEditorUtils.bindSelection(typeCmb, sourceMapper, "type", ctx);
//			FormEditorUtils.bindSelection(bitCmb, sourceMapper, "bitDepth", ctx);
			ctx.bindValue(WidgetProperties.singleSelectionIndex().observe(typeCmb), PojoProperties.value("type").observe(sourceMapper));
			ctx.bindValue(WidgetProperties.singleSelectionIndex().observe(bitCmb), PojoProperties.value("bitDepth").observe(sourceMapper));
		}
		
		return ctx;
	}

	public class ChannelSourceMapper {
		
		// Translates the channel source ID to (and from) the index in the channel source combo.
		// Translates the channel bit depth ID to (and from) the index in the bit depth combo.
		// Translates the channel type ID to (and from) the index in the channel type combo.
		
		private ImageChannel channel;

		public final int[] SOURCES = {
			ImageChannel.CHANNEL_SOURCE_JP2K, ImageChannel.CHANNEL_SOURCE_HDF5 
		};
		
		public final int[] BIT_DEPTHS = {
			ImageChannel.CHANNEL_BIT_DEPTH_1, ImageChannel.CHANNEL_BIT_DEPTH_8, ImageChannel.CHANNEL_BIT_DEPTH_16
		};
		
		public ChannelSourceMapper(ImageChannel channel) {
			this.channel = channel;
		}
		
		public int getSource() {
			return CollectionUtils.find(SOURCES, channel.getSource());
		}
		
		public void setSource(int source) {
			channel.setSource(SOURCES[source]);
		}
		
		public int getBitDepth() {
			return CollectionUtils.find(BIT_DEPTHS, channel.getBitDepth());
		}
		
		public void setBitDepth(int bitDepth) {
			channel.setBitDepth(BIT_DEPTHS[bitDepth]);
		}
		
		public int getType() {
			return channel.getType();
		}
		
		public void setType(int type) {
			channel.setType(getCurrentComponentType().getId());
		}
	}
	
}