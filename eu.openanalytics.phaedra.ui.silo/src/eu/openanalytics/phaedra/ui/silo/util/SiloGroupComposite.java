package eu.openanalytics.phaedra.ui.silo.util;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.util.autocomplete.ComboAutoCompleteField;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.vo.SiloGroup;

public class SiloGroupComposite extends Composite {

	private Text nameTxt;
	private Text descriptionTxt;
	private Combo sharingCombo;
	private Combo pClassCombo;
	private Button exampleButton;
	private Button wellTypeRadio;
	private Button subwellTypeRadio;

	private SiloGroup siloGroup;

	private ModifyListener modifyListener;
	private SelectionListener selectionListener;

	public SiloGroupComposite(Composite parent, int style, SiloGroup siloGroupObj, boolean hasChildren) {
		super(parent, style);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this);

		this.siloGroup = siloGroupObj;

		Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Name:");

		nameTxt = new Text(this, SWT.BORDER);
		nameTxt.setText(siloGroup.getName());
		nameTxt.setTextLimit(100);
		nameTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				siloGroup.setName(nameTxt.getText());
				if (modifyListener != null) {
					modifyListener.modifyText(e);
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameTxt);

		lbl = new Label(this, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

		descriptionTxt = new Text(this, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		descriptionTxt.setText(siloGroup.getDescription() != null ? siloGroup.getDescription() :  "");
		descriptionTxt.setTextLimit(200);
		descriptionTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				siloGroup.setDescription(descriptionTxt.getText());
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(descriptionTxt);

		// Group has children, do not allowing editing advanced options.
		if (!hasChildren) {
			lbl = new Label(this, SWT.NONE);
			lbl.setText("Data type:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
			
			Composite typeContainer = new Composite(this, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(typeContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(typeContainer);

			wellTypeRadio = new Button(typeContainer, SWT.RADIO);
			wellTypeRadio.setText("Well data");
			wellTypeRadio.setSelection(siloGroup.getType() == GroupType.WELL.getType());
			wellTypeRadio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					siloGroup.setType(GroupType.WELL.getType());
					if (selectionListener != null) {
						selectionListener.widgetSelected(e);
					}
				}
			});

			subwellTypeRadio = new Button(typeContainer, SWT.RADIO);
			subwellTypeRadio.setText("Subwell data");
			subwellTypeRadio.setSelection(siloGroup.getType() == GroupType.SUBWELL.getType());
			subwellTypeRadio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					siloGroup.setType(GroupType.SUBWELL.getType());
					if (selectionListener != null) {
						selectionListener.widgetSelected(e);
					}
				}
			});

			if (siloGroup.getProtocolClass() == null) {
				lbl = new Label(this, SWT.NONE);
				lbl.setText("Protocol Class:");

				final List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
				Collections.sort(pClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
				String[] pClassNames = new String[pClasses.size()];
				int index = 0;
				for (ProtocolClass pClass : pClasses) {
					pClassNames[index++] = pClass.getName();
				}
				pClassCombo = new Combo(this, SWT.NONE);
				pClassCombo.setItems(pClassNames);
				pClassCombo.select(0);
				pClassCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						ProtocolClass pClass =  pClasses.get(pClassCombo.getSelectionIndex());
						siloGroup.setProtocolClass(pClass);
						if (selectionListener != null) {
							selectionListener.widgetSelected(e);
						}
					}
				});
				GridDataFactory.fillDefaults().grab(true, false).applyTo(pClassCombo);
				new ComboAutoCompleteField(pClassCombo);

				siloGroup.setProtocolClass(pClasses.get(0));
			}
			
			lbl = new Label(this, SWT.NONE);
			lbl.setText("Sharing:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

			sharingCombo = new Combo(this, SWT.CHECK);
			sharingCombo.setItems(AccessScope.getScopeNames());
			sharingCombo.select(AccessScope.getScopeIndex(siloGroup.getAccessScope()));
			sharingCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selected = sharingCombo.getSelectionIndex();
					AccessScope scope = AccessScope.values()[selected];
					siloGroup.setAccessScope(scope);
					// If AccessScope is not public, do not allow Example.
					exampleButton.setEnabled(scope.isPublicScope());
					// If AccessScope is not public, set false, else maintain current value.
					exampleButton.setSelection(scope.isPublicScope() && exampleButton.getSelection());
					if (selectionListener != null) {
						selectionListener.widgetSelected(e);
					}
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(sharingCombo);

			lbl = new Label(this, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

			exampleButton = new Button(this, SWT.CHECK);
			exampleButton.setText("Example Silo");
			exampleButton.setToolTipText("When checked this Silo will be shown as an Example.");
			exampleButton.setEnabled(siloGroup.getAccessScope().isPublicScope());
			exampleButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					siloGroup.setExample(exampleButton.getSelection());
					if (selectionListener != null) {
						selectionListener.widgetSelected(e);
					}
				}
			});
		}

	}

	/**
	 * Add a ModifyListener. Adding a new listener will overwrite the previous one.
	 * This listener will be called when the Silo name is changed.
	 *
	 * @param listener
	 */
	public void addModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	/**
	 * Add a SelectionListener. Adding a new listener will overwrite the previous one.
	 * This listener will be called when core changes are made to the silo.
	 *
	 * @param listener
	 */
	public void addSelectionListener(SelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

}