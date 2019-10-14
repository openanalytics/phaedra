package eu.openanalytics.phaedra.base.ui.search;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;


public abstract class AbstractRealQueryValuePanelFactory extends AbstractQueryValuePanelFactory {
	
	
	public static interface PanelExtension {
		
		void dispose();
		
	}
	
	protected abstract class Panel extends Composite implements DisposeListener {
		
		protected final QueryEditor queryEditor;
		protected final QueryFilter queryFilter;
		
		private IConverter modelToUiConverter;
		private IConverter uiToModelConverter;
		
		private PanelExtension extension;
		
		private boolean isReady;
		private int inModelToUiUpdate;
		
		
		public Panel(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
			super(parent, SWT.NONE);
			this.queryEditor = queryEditor;
			this.queryFilter = queryFilter;
			
			this.extension = createExtension(this);
			
			createContent();
			
			this.isReady = true;
			updateModelToUi();
			
			addDisposeListener(this::widgetDisposed);
		}
		
		@Override
		public void widgetDisposed(final DisposeEvent e) {
			this.isReady = false;
			if (this.extension != null) {
				this.extension.dispose();
				this.extension = null;
			}
		}
		
		
		public void setConverter(final /*@Nullable*/ IConverter modelToUiConverter,
				final /*@Nullable*/ IConverter uiToModelConverter) {
			this.modelToUiConverter = modelToUiConverter;
			this.uiToModelConverter = uiToModelConverter;
			
			updateModelToUi();
		}
		
		protected String convertModelToUi(final Double value) {
			Object o = value;
			if (this.modelToUiConverter != null) {
				o = this.modelToUiConverter.convert(value);
			}
			if (o == null) {
				return "";
			}
			return o.toString();
		}
		
		protected Double convertUiToModel(final String input) {
			try {
				Double value = Double.parseDouble(input);
				if (this.uiToModelConverter != null) {
					value = (Double)this.uiToModelConverter.convert(value);
				}
				return value;
			}
			catch (final NumberFormatException e) {
				return null;
			}
		}
		
		
		protected abstract void createContent();
		
		protected Text createText(final Composite parent, final QueryEditor queryEditor, final QueryFilter queryFilter) {
			final Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
			text.addModifyListener(this::updateUiToModel);
			text.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(final FocusEvent e) {
					updateModelToUi();
				}
			});
			text.addKeyListener(queryEditor.getDirtyKeyAdapter());
			return text;
		}
		
		
		protected final void updateModelToUi() {
			if (!this.isReady) {
				return;
			}
			this.inModelToUiUpdate++;
			try {
				execUpdateModelToUi();
			} finally {
				this.inModelToUiUpdate--;
			}
		}
		
		protected final void updateUiToModel(final ModifyEvent event) {
			if (!this.isReady || this.inModelToUiUpdate > 0) {
				return;
			}
			execUpdateUiToModel(event);
		}
		
		protected abstract void execUpdateModelToUi();
		
		protected abstract void execUpdateUiToModel(ModifyEvent event);
		
	}
	
	
	public AbstractRealQueryValuePanelFactory() {
	}
	
	
	protected PanelExtension createExtension(final Panel panel) {
		return null;
	}
	
}
