package eu.openanalytics.phaedra.base.ui.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;


/**
 * Query value panel factory for multiple real values.
 */
public abstract class RealValuesQueryValuePanelFactory extends AbstractRealQueryValuePanelFactory {
	
	
	private final static Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
	
	
	protected class ListPanel extends Panel {
		
		private Text text;
		
		public ListPanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
			super(parent, queryEditor, queryFilter);
			
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(this);
		}
		
		@Override
		protected void createContent() {
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(this);
			
			this.text = createText(this, this.queryEditor, this.queryFilter);
			this.text.setToolTipText("Add a comma-separated list of values");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(this.text);
		}
		
		@Override
		protected void execUpdateModelToUi() {
			final List<Double> values = (List<Double>)this.queryFilter.getValue();
			final StringBuilder sb = new StringBuilder();
			for (final Double value : values) {
				final String s = convertModelToUi(value);
				sb.append(s);
				sb.append(", ");
			}
			this.text.setText(sb.toString());
		}
		
		@Override
		protected void execUpdateUiToModel(final ModifyEvent event) {
			final String[] split = SPLIT_PATTERN.split(this.text.getText());
			final ArrayList<Double> values = new ArrayList<Double>();
			for (final String s : split) {
				final Double value = convertUiToModel(s);
				if (value != null) {
					values.add(value);
				}
			}
			values.sort(null);
			this.queryFilter.setValue(values);
		}
		
	}
	
	
	public RealValuesQueryValuePanelFactory() {
	}
	
	
	@Override
	public boolean checkValue(final QueryFilter queryFilter) {
		final Serializable value = queryFilter.getValue();
		return (value instanceof List && ( ((List<?>) value).isEmpty() || ((List<?>) value).get(0) instanceof Double));
	}
	
	@Override
	public void clearValue(final QueryFilter queryFilter) {
		queryFilter.setValue(new ArrayList<Double>());
	}
	
	@Override
	public Composite createQueryValuePanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
		return new ListPanel(parent, queryEditor, queryFilter);
	}
	
}
