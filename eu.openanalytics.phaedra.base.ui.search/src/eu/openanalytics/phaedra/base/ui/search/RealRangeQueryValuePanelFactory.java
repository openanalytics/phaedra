package eu.openanalytics.phaedra.base.ui.search;

import java.io.Serializable;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;


/**
 * Query value panel factory for ranges of real values.
 */
public abstract class RealRangeQueryValuePanelFactory extends AbstractRealQueryValuePanelFactory {
	
	
	protected class RangePanel extends Panel {
		
		private Text text0;
		private Text text1;
		
		public RangePanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
			super(parent, queryEditor, queryFilter);
			
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, true).applyTo(this);
		}
		
		@Override
		protected void createContent() {
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
			
			this.text0 = createText(this, this.queryEditor, this.queryFilter);
			this.text1 = createText(this, this.queryEditor, this.queryFilter);
			final GridDataFactory gdFactory = GridDataFactory.fillDefaults().grab(true, false).hint(80, SWT.DEFAULT);
			gdFactory.applyTo(this.text0);
			gdFactory.applyTo(this.text1);
		}
		
		@Override
		protected void execUpdateModelToUi() {
			final Double[] values = (Double[])this.queryFilter.getValue();
			final String s1 = convertModelToUi(values[0]);
			final String s2 = convertModelToUi(values[1]);
			this.text0.setText(s1);
			this.text1.setText(s2);
		}
		
		@Override
		protected void execUpdateUiToModel(final ModifyEvent event) {
			final Double[] values = (Double[])this.queryFilter.getValue();
			final Text text = (Text)event.widget;
			final int idx = (text == this.text0) ? 0 : 1;
			final Double value = convertUiToModel(text.getText());
			if (value != null) {
				values[idx] = value;
			}
		}
		
	}
	
	
	public RealRangeQueryValuePanelFactory() {
	}
	
	
	@Override
	public boolean checkValue(final QueryFilter queryFilter) {
		final Serializable value = queryFilter.getValue();
		return (value instanceof Double[] && ((Double[]) value).length == 2);
	}
	
	@Override
	public void clearValue(final QueryFilter queryFilter) {
		queryFilter.setValue(new Double[2]);
	}
	
	@Override
	public Composite createQueryValuePanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
		return new RangePanel(parent, queryEditor, queryFilter);
	}
	
}
