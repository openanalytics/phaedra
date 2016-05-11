package eu.openanalytics.phaedra.ui.protocol.editor.page;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.protocol.editor.ProtocolClassEditor;


public class ImageSettingsPage extends FormPage {

	private WritableList inputList;
	private ImageSettings imageSettings;
	
	private CCombo imageScaleCmb;
	private Scale gammaScale;
	private Label gammaLabel;
	private Text pixelSizeXTxt;
	private Text pixelSizeYTxt;
	private Text pixelSizeZTxt;
	
	private ImageSettingsMasterBlock masterblock;

	private boolean dirty = false;

	public ImageSettingsPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
		this.imageSettings = ((ProtocolClassEditor)editor).getProtocolClass().getImageSettings();
		this.inputList = new WritableList(imageSettings.getImageChannels(), ImageChannel.class);
	}
	
	public void markEditorDirty() {
		dirty = true;
		if (getEditor() instanceof ProtocolClassEditor) {
			((ProtocolClassEditor) getEditor()).markDirty();
			return;
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public Action getSaveAction() {
		if (getEditor() instanceof ProtocolClassEditor) {
			return ((ProtocolClassEditor) getEditor()).getSaveAction();
		}
		return null;
	}

	public ImageSettings getImageSettings() {
		return imageSettings;
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		
		FormToolkit toolkit = managedForm.getToolkit();
		
		ScrolledForm form = managedForm.getForm();
		toolkit.paintBordersFor(form);
		form.setText("Image Settings");
		form.setImage(IconManager.getIconImage("channel.png"));
		Composite body = form.getBody();
		toolkit.adapt(body);
		body.setLayout(new GridLayout(2, true));
		toolkit.paintBordersFor(body);

		toolkit.decorateFormHeading(managedForm.getForm().getForm());
		managedForm.getForm().getToolBarManager().add(getSaveAction());
		managedForm.getForm().getToolBarManager().update(true);
		
		Composite topPart = new Composite(body, SWT.NONE);
		topPart.setLayout(new GridLayout(2, true));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(topPart);
		toolkit.adapt(topPart);
		
		// General Image Settings
		
		Section generalImageSettingsSection = toolkit.createSection(topPart, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		generalImageSettingsSection.marginHeight = 5;
		generalImageSettingsSection.marginWidth = 5;
		GridData gd_generalImageSettingsSection = new GridData(SWT.FILL, SWT.CENTER, true, false);
		generalImageSettingsSection.setLayoutData(gd_generalImageSettingsSection);
		generalImageSettingsSection.setText("General Image Settings");
		
		Composite compositeGeneralImage = toolkit.createComposite(generalImageSettingsSection, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = 10;
		gridLayout.marginBottom = 10;
		gridLayout.numColumns = 3;
		compositeGeneralImage.setLayout(gridLayout);
		generalImageSettingsSection.setClient(compositeGeneralImage);
		toolkit.paintBordersFor(compositeGeneralImage);

		Label label = toolkit.createLabel(compositeGeneralImage, "Default image scale (1/x):", SWT.NONE);
		label.setLayoutData(new GridData(150, SWT.DEFAULT));

		imageScaleCmb = new CCombo(compositeGeneralImage, SWT.BORDER | SWT.READ_ONLY);
		imageScaleCmb.setLayoutData(new GridData(80, SWT.DEFAULT));
		toolkit.adapt(imageScaleCmb, true, true);
		imageScaleCmb.setItems(new String[]{"1","2","4","8","16"});
		imageScaleCmb.setVisibleItemCount(10);
		
		toolkit.createLabel(compositeGeneralImage, "");

		Label gammaLabel = toolkit.createLabel(compositeGeneralImage, "Gamma:", SWT.NONE);
		GridData gd_gammaLabel = new GridData();
		gammaLabel.setLayoutData(gd_gammaLabel);

		gammaScale = new Scale(compositeGeneralImage, SWT.NONE);
		gammaScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ImageSettingsPage.this.gammaLabel.setText(String.valueOf(gammaScale.getSelection() / 10f));
			}
		});
		toolkit.adapt(gammaScale, true, true);
		gammaScale.setMinimum(1);
		gammaScale.setMaximum(70);
		gammaScale.setIncrement(10);

		String gamma = "2.5";
		if (imageSettings != null) gamma = String.valueOf(imageSettings.getGamma() / 10f);
		this.gammaLabel = toolkit.createLabel(compositeGeneralImage, gamma, SWT.NONE);
		
		label = toolkit.createLabel(compositeGeneralImage, "Pixel size (µm² per pixel):", SWT.NONE);
		
		Composite c = toolkit.createComposite(compositeGeneralImage);
		GridDataFactory.fillDefaults().grab(true, false).span(2,1).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(c);
		toolkit.createLabel(c, "X:");
		pixelSizeXTxt = toolkit.createText(c, "1", SWT.BORDER);
		GridDataFactory.fillDefaults().hint(30, SWT.DEFAULT).applyTo(pixelSizeXTxt);
		toolkit.createLabel(c, "Y:");
		pixelSizeYTxt = toolkit.createText(c, "1", SWT.BORDER);
		GridDataFactory.fillDefaults().hint(30, SWT.DEFAULT).applyTo(pixelSizeYTxt);
		toolkit.createLabel(c, "Z:");
		pixelSizeZTxt = toolkit.createText(c, "1", SWT.BORDER);
		GridDataFactory.fillDefaults().hint(30, SWT.DEFAULT).applyTo(pixelSizeZTxt);
		
		// The master-detail block for channel configuration
		
		masterblock = new ImageSettingsMasterBlock(inputList, this);
		masterblock.createContent(managedForm);

		initDataBindings();
		compositeGeneralImage.setEnabled(getEditor().isSaveAsAllowed());
		
		// Dirty listeners go last, otherwise data binding init will trigger them immediately.
		
		SelectionListener selectionDirtyListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markEditorDirty();
			}
		};
		ModifyListener textDirtyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				markEditorDirty();
			}
		};
		pixelSizeXTxt.addModifyListener(textDirtyListener);
		pixelSizeYTxt.addModifyListener(textDirtyListener);
		pixelSizeZTxt.addModifyListener(textDirtyListener);
		imageScaleCmb.addSelectionListener(selectionDirtyListener);
		gammaScale.addSelectionListener(selectionDirtyListener);
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext ctx = new DataBindingContext();
		
		ctx.bindValue(WidgetProperties.selection().observe(gammaScale), PojoProperties.value("gamma").observe(imageSettings));
		ctx.bindValue(WidgetProperties.selection().observe(imageScaleCmb), PojoProperties.value("zoomRatio").observe(imageSettings));

		UpdateValueStrategy widgetToModel = new UpdateValueStrategy() {
			@Override
			public Object convert(Object value) {
				if (value instanceof String) return Float.parseFloat(((String)value).replace(',','.'));
				return super.convert(value);
			}
		};
		UpdateValueStrategy modelToWidget = new UpdateValueStrategy() {
			@Override
			public Object convert(Object value) {
				if (value instanceof Float) return ((Float)value).toString();
				return super.convert(value);
			}
		};
		
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(pixelSizeXTxt), PojoProperties.value("pixelSizeX").observe(imageSettings), widgetToModel, modelToWidget);
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(pixelSizeYTxt), PojoProperties.value("pixelSizeY").observe(imageSettings), widgetToModel, modelToWidget);
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(pixelSizeZTxt), PojoProperties.value("pixelSizeZ").observe(imageSettings), widgetToModel, modelToWidget);
		
		return ctx;
	}
}
