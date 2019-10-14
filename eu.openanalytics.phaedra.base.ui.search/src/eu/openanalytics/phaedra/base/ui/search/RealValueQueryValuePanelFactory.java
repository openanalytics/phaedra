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
 * Query value panel factory for a real value.
 */
public abstract class RealValueQueryValuePanelFactory extends AbstractRealQueryValuePanelFactory {
	
	
	protected class ValuePanel extends Panel {
		
		private Text text;
		
		public ValuePanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
			super(parent, queryEditor, queryFilter);
			
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, true).applyTo(this);
		}
		
		@Override
		protected void createContent() {
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(this);
			
			this.text = createText(this, this.queryEditor, this.queryFilter);
			GridDataFactory.fillDefaults().grab(true, false).hint(80, SWT.DEFAULT)
					.applyTo(this.text);
		}
		
		@Override
		protected void execUpdateModelToUi() {
			final String s = convertModelToUi((Double)this.queryFilter.getValue());
			this.text.setText(s);
		}
		
		@Override
		protected void execUpdateUiToModel(final ModifyEvent event) {
			final Double value = convertUiToModel(this.text.getText());
			if (value != null) {
				this.queryFilter.setValue(value);
			}
		}
		
	}
	
	
	public RealValueQueryValuePanelFactory() {
	}
	
	
	@Override
	public boolean checkValue(final QueryFilter queryFilter) {
		final Serializable value = queryFilter.getValue();
		return (value == null || value instanceof Double);
	}
	
	@Override
	public void clearValue(final QueryFilter queryFilter) {
		queryFilter.setValue(null);
	}
	
	@Override
	public Composite createQueryValuePanel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
		return new ValuePanel(parent, queryEditor, queryFilter);
	}
	
}
