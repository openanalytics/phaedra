package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
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
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.PatternConfig.GroupRole;

public class PatternTester {

	private Text patternTxt;
	private FolderSelector folderSelector;
	private TreeViewer matchViewer;
	private Label matchesLbl;
	private Combo[] groupRoleCmb;
	
	private PatternConfig config;
	private Consumer<String> errorHandler;
	private TestPatternJob job;
	
	public Composite createComposite(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(5,5).numColumns(2).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Pattern:");
		
		patternTxt = new Text(area, SWT.BORDER);
		patternTxt.addModifyListener(event -> {
			config.pattern = patternTxt.getText();
			testPattern();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(patternTxt);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Folder:");
		
		folderSelector = new FolderSelector(area, SWT.PUSH);
		folderSelector.addListener(SWT.Selection, event -> {
			config.folder = folderSelector.getSelectedFolder();
			testPattern();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(folderSelector);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Contents:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		matchViewer = new TreeViewer(area);
		matchViewer.setContentProvider(new MatchesContentProvider());
		matchViewer.setLabelProvider(new MatchesLabelProvider());
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 150).applyTo(matchViewer.getControl());
		
		lbl = new Label(area, SWT.NONE);
		matchesLbl = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(matchesLbl);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Groups:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		Composite groupComp = new Composite(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupComp);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(groupComp);
		
		groupRoleCmb = new Combo[3];
		for (int i = 0; i < groupRoleCmb.length; i++) {
			new Label(groupComp, SWT.NONE).setText("Group " + (i+1) + " role:");
			groupRoleCmb[i] = new Combo(groupComp, SWT.READ_ONLY);
			final int index = i;
			groupRoleCmb[i].addListener(SWT.Selection, e -> {
				config.groupRoles[index] = GroupRole.valueOf(groupRoleCmb[index].getText());
			});
		}
		
		return area;
	}
	
	public void loadConfig(PatternConfig config, Consumer<String> errorHandler) {
		this.config = config;
		this.errorHandler = errorHandler;
		
		String newPattern = config.pattern;
		if (newPattern == null) newPattern = "";
		patternTxt.setText(newPattern);
		patternTxt.setEditable(config.patternEditable);
		
		if (config.folder != null) folderSelector.setSelectedFolder(config.folder);
		folderSelector.setEnabled(config.folderEditable);
		
		for (int i = 0; i < config.groupRoles.length; i++) {
			String[] items = Arrays.stream(GroupRole.values()).map(r -> r.toString()).toArray(n -> new String[n]);
			groupRoleCmb[i].setItems(items);
			groupRoleCmb[i].select(CollectionUtils.find(items, config.groupRoles[i].toString()));
			groupRoleCmb[i].setEnabled(config.groupsEditable);
		}
	}
	
	private void testPattern() {
		String regex = patternTxt.getText();
		if (regex.isEmpty() || config.folder == null) {
			matchesLbl.setText("No matches");
			matchViewer.setInput(null);
			return;
		}

		if (job != null) job.cancel();
		job = new TestPatternJob(regex, config.folder);
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
				updateUI(matches, "Invalid pattern");
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
	        		updateUI(matches, "Failed to list folder contents");
				});
				return Status.OK_STATUS;
			}
			
			matches.sort((m1,m2) -> m1.fileName.compareTo(m2.fileName));
			updateUI(matches, null);
			monitor.done();
			return Status.OK_STATUS;
		}
		
		private void updateUI(List<PatternMatch> matches, String msg) {
			Display.getDefault().asyncExec(() -> {
				if (matchesLbl.isDisposed()) return;
				
				errorHandler.accept(msg);
				
				long matchCount = matches.stream().filter(m -> m.isMatch).count();
				matchesLbl.setText(matchCount + " matches");
				matchViewer.setInput(matches);

				int groupCount = 0;
				if (matchCount > 0) {
					PatternMatch sample = matches.stream().filter(m -> m.isMatch).findAny().orElse(null);
					if (sample != null) groupCount = sample.groups.length;
				}
				for (int i = 0; i < groupRoleCmb.length; i++) {
					groupRoleCmb[i].setEnabled(i < groupCount && config.groupsEditable);
				}
			});
		}
	}
}
