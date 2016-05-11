package eu.openanalytics.phaedra.ui.protocol.template;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.protocol.template.validation.ValidationOutcome;

public class PatternTester {

	private Combo settingCmb;
	private Text patternTxt;
	private FolderSelector folderSelector;
	private TreeViewer matchViewer;
	private Label matchesLbl;
	
	private ValidationOutcome validation;
	private TestPatternJob job;
	
	public Composite createComposite(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(5,5).numColumns(2).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Setting:");
		
		settingCmb = new Combo(area, SWT.READ_ONLY);
		settingCmb.addListener(SWT.Selection, event -> patternTxt.setText(validation.getRegexSettings().get(settingCmb.getText())));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(settingCmb);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Pattern:");
		
		patternTxt = new Text(area, SWT.BORDER);
		patternTxt.setEditable(false);
		patternTxt.addModifyListener(event -> testPattern());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(patternTxt);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Folder:");
		
		folderSelector = new FolderSelector(area, SWT.PUSH);
		folderSelector.addListener(SWT.Selection, event -> testPattern());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(folderSelector);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Contents:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		matchViewer = new TreeViewer(area);
		matchViewer.setContentProvider(new MatchesContentProvider());
		matchViewer.setLabelProvider(new MatchesLabelProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(matchViewer.getControl());
		
		lbl = new Label(area, SWT.NONE);
		matchesLbl = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(matchesLbl);
		
		return area;
	}
	
	public void loadSettings(ValidationOutcome validation) {
		this.validation = validation;
		String[] settings = validation.getRegexSettings().keySet().toArray(new String[0]);
		Arrays.sort(settings);
		
		String currentSetting = settingCmb.getText();
		settingCmb.setItems(settings);
		if (settingCmb.getItemCount() > 0) {
			if (!currentSetting.isEmpty()) settingCmb.select(settingCmb.indexOf(currentSetting));
			else settingCmb.select(0);
		}
		
		String newPattern = validation.getRegexSettings().get(settingCmb.getText());
		if (newPattern == null) newPattern = "";
		patternTxt.setText(newPattern);
	}
	
	private void testPattern() {
		String regex = patternTxt.getText();
		String folder = folderSelector.getSelectedFolder();
		if (regex.isEmpty() || folder == null) {
			matchesLbl.setText("Matches: 0");
			matchViewer.setInput(null);
			return;
		}

		if (job != null) job.cancel();
		job = new TestPatternJob(regex, folder);
		job.schedule();
	}

	private static class MatchesContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Collection<?>) return ((Collection<?>)inputElement).toArray(new Object[0]);
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element instanceof Collection<?>);
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}
		
		@Override
		public Object getParent(Object element) {
			return null;
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}
		
		@Override
		public void dispose() {
			// Nothing to dispose.
		}
	}
	
	private static class MatchesLabelProvider extends StyledCellLabelProvider {
		
		private final static TextStyle STYLE_MATCH = new TextStyle(null, Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN), null);
		private final static TextStyle STYLE_NO_MATCH = new TextStyle(null, Display.getDefault().getSystemColor(SWT.COLOR_RED), null);
		private final static TextStyle STYLE_GROUP = new TextStyle(null, Display.getDefault().getSystemColor(SWT.COLOR_BLUE), null);
		
		@Override
		public void update(ViewerCell cell) {
			PatternMatch match = (PatternMatch)cell.getElement();
			if (match.isMatch) cell.setStyleRanges(SWTUtils.createStyleRanges(match.fileName.length(), STYLE_MATCH, STYLE_GROUP, match.groups));
			else cell.setStyleRanges(new StyleRange[] { SWTUtils.createStyleRange(0, match.fileName.length(), STYLE_NO_MATCH) });
			cell.setText(match.fileName);
		}
	}
	
	private static class PatternMatch {
		public String fileName;
		public boolean isMatch;
		public Point[] groups;
	}
	
	private class TestPatternJob extends Job {

		private String regex;
		private String folder;
		
		public TestPatternJob(String regex, String folder) {
			super("Testing Pattern");
			this.regex = regex;
			this.folder = folder;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Testing Pattern", IProgressMonitor.UNKNOWN);
			List<PatternMatch> matches = new ArrayList<>();
			
			monitor.subTask("Compiling pattern");
			Pattern pattern = null;
			try {
				pattern = Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				Display.getDefault().asyncExec(() -> {
					matchesLbl.setText("Invalid pattern");
					matchViewer.setInput(matches);
				});
				return Status.OK_STATUS;
			}

			monitor.subTask("Matching folder contents");
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folder))) {
	            for (Path path : directoryStream) {
	            	if (monitor.isCanceled()) return Status.CANCEL_STATUS;
	            	PatternMatch match = new PatternMatch();
	            	match.fileName = path.getFileName().toString();
	            	matches.add(match);
	            	
	            	if (pattern == null) continue;
	            	Matcher matcher = pattern.matcher(match.fileName);
	            	match.isMatch = matcher.matches();
	            	if (!match.isMatch) continue;
	            	
	        		MatchResult res = matcher.toMatchResult();
	        		match.groups = new Point[res.groupCount()];
	        		for (int i=0; i<match.groups.length; i++) {
	        			match.groups[i] = new Point(res.start(i+1), res.end(i+1)-1);
	            	}
	            }
	        } catch (IOException e) {
	        	Display.getDefault().asyncExec(() -> {
					matchesLbl.setText("Failed to list folder contents");
					matchViewer.setInput(matches);
				});
				return Status.OK_STATUS;
			}
			
			matches.sort((m1,m2) -> m1.fileName.compareTo(m2.fileName));
			
			Display.getDefault().asyncExec(() -> {
				long matchCount = matches.stream().filter(m -> m.isMatch).count();
				matchesLbl.setText("Matches: " + matchCount);
				matchViewer.setInput(matches);
			});

			monitor.done();
			return Status.OK_STATUS;
		}
	}
}
