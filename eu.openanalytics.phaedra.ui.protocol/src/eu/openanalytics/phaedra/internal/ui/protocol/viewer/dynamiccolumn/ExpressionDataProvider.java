package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.ui.protocol.Activator;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.EvaluationContext;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptLanguage;


public class ExpressionDataProvider<TEntity, TViewerElement> extends DataProvider<TEntity, TViewerElement> {
	
	
	private final ScriptLanguage language;
	private final String code;
	
	private final EvaluationContext<TEntity> evaluationContext;
	
	
	public ExpressionDataProvider(final DataDescription dataDescription,
			final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput,
			final ScriptLanguage language, final String code,
			final EvaluationContext<TEntity> evaluationContext) {
		super(dataDescription, viewerInput);
		this.language = language;
		this.code = code;
		
		this.evaluationContext = evaluationContext;
	}
	
	
	@Override
	public Object apply(final TEntity element) {
		try {
			final ExpressionScriptContext context = new ExpressionScriptContext(this.language);
			this.evaluationContext.contributeVariables(context, element);
			final Object data = ScriptService.getInstance().executeScript(
					this.code, context.getVariables(), this.language.getId() );
			return checkData(element, data);
		} catch (final ScriptException e) {
			EclipseLog.log(new Status(IStatus.INFO, Activator.PLUGIN_ID,
					"Computing value for dynamic column failed.\n" + toString(), e ),
					Activator.getDefault() );
			return DataLoadStatus.error(e.getMessage());
		}
	}
	
	
	@Override
	public int hashCode() {
		int hash = this.language.hashCode();
		hash = 31 * hash + this.code.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			final ExpressionDataProvider<?, ?> other = (ExpressionDataProvider<?, ?>)obj;
			return (this.language.equals(other.language)
					&& this.code.equals(other.code) );
		}
		return false;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ExpressionDataProvider");
		sb.append(" language= ").append(this.language.getId());
		sb.append(" code= \n").append(this.code);
		return sb.toString();
	}
	
}
