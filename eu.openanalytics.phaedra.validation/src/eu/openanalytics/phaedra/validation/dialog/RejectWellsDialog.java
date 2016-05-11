package eu.openanalytics.phaedra.validation.dialog;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationUtils;
import eu.openanalytics.phaedra.validation.ValidationService.Action;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

/**
 * This dialog asks the user to provide the input needed to reject one or more wells:
 * <ul>
 * <li>The rejection status: one of the available rejection codes</li>
 * <li>A rejection reason: a free-text reason that accompanies the rejection status</li>
 * </ul>
 * If both are provided, an attempt is made to apply the status to the selected well(s).
 * This may fail if for example the plate is validated or approved.
 */
public class RejectWellsDialog extends BaseValidationDialog {

	private List<Well> wells;
	
	private ComboViewer statusComboViewer;
	private StyledText statusInfoText;
	
	public RejectWellsDialog(Shell parentShell, List<Well> wells) {
		super(parentShell);
		this.wells = wells;
	}

	@Override
	protected void fillDialogArea(Composite container) {
		setMessage("Specify a rejection status and provide a reason for the rejection.");
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Status:");
		
		statusComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		statusComboViewer.addSelectionChangedListener(e -> statusSelected());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(statusComboViewer.getControl());
		
		new Label(container, SWT.NONE);
		
		statusInfoText = new StyledText(container, SWT.BORDER | SWT.WRAP);
		statusInfoText.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(statusInfoText);
	}

	@Override
	protected String getTitle() {
		return "Reject Well(s)";
	}
	
	@Override
	protected String getHistoryKey() {
		return "reason_history_reject_well";
	}
	
	@Override
	protected void initFields() {
		WellStatus[] statusCodes = Arrays.stream(WellStatus.values())
				.filter(s -> s.getCode() < -2)
				.sorted((x,y) -> x.getCode() - y.getCode())
				.toArray(x -> new WellStatus[x]);
		statusComboViewer.setContentProvider(new ArrayContentProvider());
		statusComboViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) { return ((WellStatus)element).getLabel(); };
		});
		statusComboViewer.setInput(statusCodes);
		statusComboViewer.setSelection(new StructuredSelection(statusCodes[0]));
		
		statusSelected();
		super.initFields();
	}
	
	@Override
	protected void doAction(String reason) {
		WellStatus status = SelectionUtils.getFirstObject(statusComboViewer.getSelection(), WellStatus.class);
		Action action = (status == WellStatus.REJECTED_PHAEDRA) ? Action.REJECT_WELL : Action.REJECT_OUTLIER_WELL;
		ValidationJobHelper.doInJob(action, reason, () -> ValidationUtils.applyRejectReason(wells, reason), wells);
	}
	
	private void statusSelected() {
		WellStatus selection = SelectionUtils.getFirstObject(statusComboViewer.getSelection(), WellStatus.class);
		statusInfoText.setText(selection.getDescription()); 
	}
}
