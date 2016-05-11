package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

/**
 * A Composite that allows the user to select a folder.
 * The folder can be entered or pasted into a field, or
 * the folder can be selected by using a folder selection dialog.
 */
public class FolderSelector extends Composite {

	private Text folderTxt;
	private Button changeFolderBtn;
	
	private String selectedFolder;
	
	public FolderSelector(Composite parent, int style) {
		super(parent, style);
		
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(this);
		
		Label lbl = new Label(this, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("folder.png"));
		
		folderTxt = new Text(this, SWT.BORDER);
		folderTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String txt = folderTxt.getText();
				selectedFolder = txt;
				sendSelectionEvent();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(folderTxt);
		
		changeFolderBtn = new Button(this, SWT.PUSH);
		changeFolderBtn.setText("Change...");
		changeFolderBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Select a folder below:");
				dialog.setFilterPath(selectedFolder);
				String newFolder = dialog.open();
				if (newFolder != null) {
					folderTxt.setText(newFolder);
				}
			}
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		folderTxt.setEnabled(enabled);
		changeFolderBtn.setEnabled(enabled);
		super.setEnabled(enabled);
	}
	
	public String getSelectedFolder() {
		return selectedFolder;
	}
	
	public void setSelectedFolder(String selectedFolder) {
		this.selectedFolder = selectedFolder;
		if (selectedFolder != null) {
			folderTxt.setText(selectedFolder);
		} else {
			folderTxt.setText("");
		}
	}
	
	public void addSelectionListener(SelectionListener listener) {
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
	}
	
	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
	}
	
	private void sendSelectionEvent() {
		Event event = new Event();
		event.data = selectedFolder;
		event.widget = this;
		event.type = SWT.Selection;
		
		Listener[] listeners = getListeners(SWT.Selection);
		for (Listener l: listeners) {
			l.handleEvent(event);
		}
	}
}
