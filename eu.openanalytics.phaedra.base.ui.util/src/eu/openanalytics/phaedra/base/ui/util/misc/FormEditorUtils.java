package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.ObservableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class FormEditorUtils {

	private static Font valueLabelFont = new Font(null, "Tahoma", 8, SWT.BOLD);

	public static FormToolkit createToolkit() {
		return new FormToolkit(Display.getDefault());
	}

	public static Form createForm(String text, int columns, Composite parent, FormToolkit tk) {
		Form form = tk.createForm(parent);
		tk.paintBordersFor(form);
		form.setText(text);
		GridLayoutFactory.fillDefaults().numColumns(columns).margins(5,5).applyTo(form.getBody());
		return form;
	}

	public static ScrolledForm createScrolledForm(String text, int columns, Composite parent, FormToolkit tk) {
		ScrolledForm scrolledForm = tk.createScrolledForm(parent);
		tk.paintBordersFor(scrolledForm);
		scrolledForm.setText(text);
		GridLayoutFactory.fillDefaults().numColumns(columns).margins(5,5).applyTo(scrolledForm.getBody());
		return scrolledForm;
	}

	public static Section createSection(String text, Composite parent, FormToolkit tk) {
		return createSection(text, parent, tk, false);
	}

	/**
	 * Same as a regular Section except this section will resize the parent when it expands so other section will take up the new available space.
	 * @param text Section text
	 * @param parent Parent component
	 * @param tk FormToolkit
	 * @param enableSmartGrab Enable smart grab
	 * @return
	 */
	public static Section createSection(String text, Composite parent, FormToolkit tk, boolean enableSmartGrab) {
		final Section section = tk.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		section.setText(text);
		if (enableSmartGrab) {
			section.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					GridData gridData = (GridData) section.getLayoutData();
					gridData.grabExcessVerticalSpace = e.getState();
					section.getParent().layout();
				}
			});
		}
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(section);
		return section;
	}

	public static Composite createComposite(int columns, Section parent, FormToolkit tk) {
		Composite composite = tk.createComposite(parent, SWT.WRAP);
		tk.paintBordersFor(composite);
		parent.setClient(composite);
		GridLayoutFactory.fillDefaults().numColumns(columns).applyTo(composite);
		return composite;
	}

	public static Label createLabel(String text, Composite parent, FormToolkit tk) {
		Label lbl = tk.createLabel(parent, text, SWT.WRAP);
		return lbl;
	}

	public static Label createValueLabel(String text, Composite parent, FormToolkit tk) {
		Label lbl = tk.createLabel(parent, text, SWT.WRAP);
		lbl.setFont(valueLabelFont);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(lbl);
		return lbl;
	}

	public static Label createLabelPair(String lbl, Composite parent, FormToolkit tk) {
		return createLabelPair(lbl, "", parent, tk);
	}

	public static Label createLabelPair(String lbl, String value, Composite parent, FormToolkit tk) {
		createLabel(lbl, parent, tk);
		return createValueLabel(value, parent, tk);
	}

	public static Text createLabelTextPair(String lbl, Composite parent, FormToolkit tk) {
		return createLabelTextPair(lbl, "", parent, tk);
	}

	public static Text createLabelTextPair(String lbl, String value, Composite parent, FormToolkit tk) {
		createLabel(lbl, parent, tk);
		Text txt = tk.createText(parent, value, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,  false).applyTo(txt);
		return txt;
	}

	public static Hyperlink createHyperlink(Composite parent, Runnable onClick) {
		Hyperlink link = new Hyperlink(parent, SWT.NONE);
		link.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		link.setUnderlined(true);
		link.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				onClick.run();
			}
		});
		return link;
	}
	
	public static void bindText(Control ctrl, Object pojo, String varName, DataBindingContext ctx) {
		IObservableValue target = WidgetProperties.text(SWT.Modify).observe(ctrl);
		IObservableValue model = PojoProperties.value(varName).observe(pojo);
		ctx.bindValue(target, model);
	}

	public static void bindSelection(Control ctrl, Object pojo, String varName, DataBindingContext ctx) {
		IObservableValue target = WidgetProperties.selection().observe(ctrl);
		IObservableValue model = PojoProperties.value(varName).observe(pojo);
		ctx.bindValue(target, model);
	}
	
	public static <K,V> void bindTextToMap(Control ctrl, ObservableMap<K,V> map, K key, DataBindingContext ctx) {
		IObservableValue target = WidgetProperties.text(SWT.Modify).observe(ctrl);
		IObservableValue model = Observables.observeMapEntry(map, key);
		ctx.bindValue(target, model);	
	}
	
	public static <K,V> void bindSelectionToMap(Control ctrl, ObservableMap<K,V> map, K key, DataBindingContext ctx) {
		IObservableValue target = WidgetProperties.selection().observe(ctrl);
		IObservableValue model = Observables.observeMapEntry(map, key);
		ctx.bindValue(target, model);	
	}
	
	public static void clearBindings(DataBindingContext ctx) {
		if (ctx != null) {
			Object[] list = ctx.getBindings().toArray();
			for (Object object : list) {
				Binding binding = (Binding) object;
				ctx.removeBinding(binding);
				binding.dispose();
			}
			ctx.dispose();
		}
	}
}