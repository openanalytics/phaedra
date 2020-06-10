package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;


public class OptionStack extends Composite {
	
	
	public static class Option {
		
		
		private OptionStack parent;
		
		private Composite composite;
		
		private final List<Binding> dataBindings = new ArrayList<>();
		
		
		public Option() {
		}
		
		
		public boolean isActive() {
			final OptionStack parent = this.parent;
			return (parent != null && parent.active == this);
		}
		
		private Composite activate(final OptionStack parent) {
			if (this.parent != null && this.parent != parent) {
				throw new IllegalStateException();
			}
			Composite composite;
			if (this.parent != parent) {
				this.parent = parent;
				composite = createControls();
				this.composite = composite;
			}
			else {
				composite = this.composite;
				final DataBindingContext dbc = parent.dataBindingContext;
				if (dbc != null) {
					for (final Binding binding : this.dataBindings) {
						dbc.addBinding(binding);
					}
				}
			}
			return composite;
		}
		
		private void deactivate() {
			final OptionStack parent = this.parent;
			final DataBindingContext dbc = parent.dataBindingContext;
			if (dbc != null) {
				for (final Binding binding : this.dataBindings) {
					dbc.removeBinding(binding);
				}
			}
		}
		
		private Composite createControls() {
			final OptionStack parent = this.parent;
			final DataBindingContext dbc = parent.dataBindingContext;
			final Composite composite = createControls(parent);
			if (composite != null && dbc != null) {
				final IListChangeListener<Binding> bindingsListener= new IListChangeListener<Binding>() {
					@Override
					public void handleListChange(final ListChangeEvent<? extends Binding> event) {
						for (final ListDiffEntry<? extends Binding> diff : event.diff.getDifferences()) {
							if (diff.isAddition()) {
								dataBindings.add(diff.getElement());
							}
						}
					}
				};
				dbc.getBindings().addListChangeListener(bindingsListener);
				try {
					initDataBinding(dbc);
				}
				finally {
					dbc.getBindings().removeListChangeListener(bindingsListener);
				}
				initInput();
				updateTargets();
				updateModels();
				
				if (parent.listener != null) {
					for (final Binding binding : this.dataBindings) {
						binding.getModel().addChangeListener(parent.listener);
					}
				}
			}
			return composite;
		}
		
		protected Composite createControls(final Composite parent) {
			return null;
		}
		
		protected void initDataBinding(final DataBindingContext dbc) {
		}
		
		protected void initInput() {
		}
		
		protected void updateModels() {
			for (final Binding binding : this.dataBindings) {
				binding.updateTargetToModel();
			}
		}
		
		protected void updateTargets() {
			for (final Binding binding : this.dataBindings) {
				binding.updateModelToTarget();
			}
		}
		
		protected IStatus getValidationStatus() {
			IStatus status = ValidationStatus.ok();
			for (final Binding binding : this.dataBindings) {
				final IStatus bindingStatus = (IStatus)binding.getValidationStatus().getValue();
				if (bindingStatus != null && bindingStatus.getSeverity() > status.getSeverity()) {
					status = bindingStatus;
				}
			}
			return status;
		}
		
	}
	
	
	private final StackLayout layout = new StackLayout();
	
	private Option active;
	
	private DataBindingContext dataBindingContext;
	
	private IChangeListener listener;
	
	
	public OptionStack(final Composite parent, final int style) {
		super(parent, style);
		setLayout(this.layout);
	}
	
	
	public void initDataBinding(final DataBindingContext dbc, final IChangeListener listener) {
		this.dataBindingContext = dbc;
		this.listener = listener;
	}
	
	public void initDataBinding(final DataBindingContext dbc) {
		initDataBinding(dbc, null);
	}
	
	
	public Option getActive() {
		return this.active;
	}
	
	public void setActive(final Option option) {
		Option prev = this.active;
		if (option == prev) {
			return;
		}
		if (prev != null) {
			prev.deactivate();
		}
		this.active = option;
		if (option != null) {
			this.layout.topControl = option.activate(this);
		}
		else {
			this.layout.topControl = null;
		}
		layout(true);
	}
	
}
