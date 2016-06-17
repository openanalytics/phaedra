package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class EditPatternDialog extends TitleAreaDialog {

	private PatternTester patternTester;
	
	private Map<String,String> patterns;
	private ImageChannel channel;
	private Path imageFolder;
	
	public EditPatternDialog(Shell parentShell, ImageChannel channel, Path imageFolder) {
		super(parentShell);
		this.channel = channel;
		this.imageFolder = imageFolder;
		
		patterns = new HashMap<>();
		patterns.put(channel.getName(), channel.getDescription());
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Pattern");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		patternTester = new PatternTester();
		patternTester.createComposite(main);
		patternTester.loadSettings(patterns, imageFolder.toFile().getAbsolutePath(), true);
		
		setTitle("Edit Pattern");
		setMessage("Adjust the pattern for the image files of this channel.");
		return main;
	}
	
	@Override
	protected void okPressed() {
		channel.setDescription(patterns.get(channel.getName()));
		super.okPressed();
	}
}
