package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

/**
 * A Composite that allows the user to select a file.
 * The file can be entered or pasted into a field, or
 * the file can be selected by using a file selection dialog.
 */
public class FileSelector extends Composite {

	private Label lbl;
	private Text fileTxt;
	private Button changeFileBtn;

	private String selectedFile;
	private String suggestedName;
	private String[] extensions;

	/**
	 * A Composite that allows the user to select a file.
	 * @param parent
	 * @param style
	 *  
	 * @see SWT#SAVE
	 * @see SWT#OPEN
	 * @see SWT#MULTI
	 */
	public FileSelector(Composite parent, int style) {
		this(parent, style, new String[] { "*.*" });
	}

	/**
	 * A Composite that allows the user to select a file.
	 * @param parent
	 * @param style
	 * @param extensions Specify the extensions (e.g. "*.png")
	 * 
	 * @see SWT#SAVE
	 * @see SWT#OPEN
	 * @see SWT#MULTI
	 */
	public FileSelector(Composite parent, final int style, String[] extensions) {
		super(parent, style);
		this.extensions = extensions;

		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(this);

		lbl = new Label(this, SWT.NONE);
		updateImage(extensions[0]);

		fileTxt = new Text(this, SWT.BORDER);
		fileTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String txt = fileTxt.getText();
				selectedFile = txt;
				sendSelectionEvent();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(fileTxt);

		changeFileBtn = new Button(this, SWT.PUSH);
		changeFileBtn.setText("Change...");
		changeFileBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), style);
				dialog.setFileName((selectedFile == null || selectedFile.isEmpty()) ? suggestedName : selectedFile);
				dialog.setFilterPath(selectedFile);
				dialog.setFilterExtensions(FileSelector.this.extensions);
				String newFolder = dialog.open();
				if (newFolder != null) {
					fileTxt.setText(newFolder);
					updateImage(FileSelector.this.extensions[0]);
				}
			}
		});
	}

	public String getSelectedFile() {
		return selectedFile;
	}

	public void setSelectedFile(String selectedFile) {
		this.selectedFile = selectedFile;
		if (selectedFile != null) {
			fileTxt.setText(selectedFile);
		} else {
			fileTxt.setText("");
		}
	}
	
	public String getSuggestedName() {
		return suggestedName;
	}
	
	public void setSuggestedName(String suggestedName) {
		this.suggestedName = suggestedName;
	}
	
	public void setExtensions(String[] extensions) {
		this.extensions = extensions;
	}

	public void addSelectionListener(SelectionListener listener) {
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
	}
	
	private void updateImage(String extension) {
		Image img;
		if (selectedFile != null) {
			// Find program based on file location.
			img = getProgramImage(Program.findProgram(selectedFile));
		} else {
			// Find program based on extension. (Filter out possible * wild card)
			img = getProgramImage(Program.findProgram(extension.replace("*", "")));
		}
		lbl.setImage(img);
	}

	private Image getProgramImage(Program p) {
		Image img;
		if (p != null) {
			img = new Image(null, p.getImageData());
		} else {
			img = IconManager.getIconImage("file.png");
		}
		return img;
	}

	private void sendSelectionEvent() {
		Event event = new Event();
		event.data = selectedFile;
		event.widget = this;
		event.type = SWT.Selection;

		Listener[] listeners = getListeners(SWT.Selection);
		for (Listener l: listeners) {
			l.handleEvent(event);
		}
	}

}