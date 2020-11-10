package eu.openanalytics.phaedra.validation.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.validation.Activator;

/**
 * An abstract base class for validation actions that require a reason to be specified.
 */
public abstract class BaseValidationDialog extends TitleAreaDialog {

	private Text reasonText;
	private ComboViewer reasonHistoryViewer;
	
	private final static String REASON_HISTORY_MESSAGE = "<Choose a previously used reason>";
	private final static String REASON_HISTORY_SEPARATOR = ":::";
	
	public BaseValidationDialog(Shell parentShell) {
		super(parentShell);
	}

	protected abstract String getHistoryKey();
	
	protected abstract void doAction(String reason);
	
	protected abstract void fillDialogArea(Composite container);
	
	protected abstract String getTitle();
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentContainer = (Composite)super.createDialogArea(parent);
		Composite container = new Composite(parentContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(container);
		
		setTitle(getTitle());
		setTitleImage(IconManager.getIconImage("thumbs_down.png"));
		
		fillDialogArea(container);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Reason:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		reasonText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		reasonText.addModifyListener(e -> checkPageComplete());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(reasonText);

		new Label(container, SWT.NONE);

		reasonHistoryViewer = new ComboViewer(container, SWT.READ_ONLY);
		reasonHistoryViewer.addSelectionChangedListener(e -> reasonSelected());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(reasonHistoryViewer.getControl());
		
		initFields();
		
		return parentContainer;
	}
	
	protected void initFields() {
		if (reasonHistoryViewer == null) return;
		List<String> recentReasons = getReasonHistory(true);
		reasonHistoryViewer.setContentProvider(new ArrayContentProvider());
		reasonHistoryViewer.setLabelProvider(new LabelProvider());
		reasonHistoryViewer.setInput(recentReasons);
		reasonHistoryViewer.setSelection(new StructuredSelection(recentReasons.get(0)));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		checkPageComplete();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(getTitle());
		newShell.setSize(600, 450);
	}
	
	@Override
	protected void okPressed() {
		saveReasonHistory();
		String reason = (reasonText == null) ? null : reasonText.getText();
		doAction(reason);
		super.okPressed();
	}
	
	private void reasonSelected() {
		if (reasonText == null) return;
		String reason = SelectionUtils.getFirstObject(reasonHistoryViewer.getSelection(), String.class);
		if (REASON_HISTORY_MESSAGE.equals(reason)) reason = "";
		reasonText.setText(reason);
	}
	
	private void checkPageComplete() {
		if (reasonText == null) return;
		boolean isComplete = !reasonText.getText().isEmpty();
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) okButton.setEnabled(isComplete);
	}
	
	private List<String> getReasonHistory(boolean includeMessage) {
		List<String> reasonHistory = new ArrayList<>();
		if (includeMessage) reasonHistory.add(REASON_HISTORY_MESSAGE);
		
		IDialogSettings dialogSettings = Activator.getDefault().getDialogSettings();
		String history = dialogSettings.get(getHistoryKey());
		if (history != null) {
			Arrays.stream(history.split(REASON_HISTORY_SEPARATOR)).filter(h -> !h.isEmpty()).forEach(h -> reasonHistory.add(h));
		}
		
		return reasonHistory;
	}
	
	private void saveReasonHistory() {
		List<String> recentReasons = getReasonHistory(false);

		if (reasonText != null && !reasonText.getText().isEmpty()) {
			CollectionUtils.addUnique(recentReasons, reasonText.getText());
		}
		
		IDialogSettings dialogSettings = Activator.getDefault().getDialogSettings();
		String history = recentReasons.stream().collect(StringBuilder::new,
				(sb,s) -> sb.append(s + REASON_HISTORY_SEPARATOR),
				(sb1,sb2) -> sb1.append(sb2.toString())).toString();
		dialogSettings.put(getHistoryKey(), history);
	}
}
