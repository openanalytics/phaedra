package eu.openanalytics.phaedra.ui.protocol.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolContentProvider;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolLabelProvider;

public class SelectProtocolDialog extends TitleAreaDialog {

	private TreeViewer protocolViewer;
	private Protocol selectedProtocol;
	
	public SelectProtocolDialog(Shell parentShell) {
		super(parentShell);
	}

	public Protocol getSelectedProtocol() {
		return selectedProtocol;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Protocol");
		newShell.setSize(400,450);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the main part of the dialog (Input)
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(container);

		protocolViewer = new TreeViewer(container);
		protocolViewer.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedProtocol = SelectionUtils.getFirstObject(protocolViewer.getSelection(), Protocol.class);
				getButton(IDialogConstants.OK_ID).setEnabled(selectedProtocol != null);
			}
		});
		GridDataFactory.fillDefaults().grab(true,true).applyTo(protocolViewer.getControl());
		protocolViewer.setContentProvider(new ProtocolContentProvider());
		protocolViewer.setLabelProvider(new ProtocolLabelProvider());
		protocolViewer.setAutoExpandLevel(0);
		protocolViewer.setInput("root");
		
		setTitle("Select Protocol");
		setMessage("Select one of the available protocols below.");

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
}
