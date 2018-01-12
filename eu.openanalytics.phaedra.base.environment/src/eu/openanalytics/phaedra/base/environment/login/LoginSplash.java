package eu.openanalytics.phaedra.base.environment.login;

import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.environment.EnvironmentRegistry;
import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.environment.config.ConfigLoader;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.ldap.LDAPUtils;
import eu.openanalytics.phaedra.base.security.ui.AccessDialog;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;

public class LoginSplash extends BasicSplashHandler {

	private static final int F_LABEL_HORIZONTAL_INDENT = 20;
	private static final int F_BUTTON_WIDTH_HINT = 80;
	private static final int F_TEXT_WIDTH_HINT = 175;
	private static final int F_COLUMN_COUNT = 3;

	private Shell splash;
	
	private Composite loginCmp;
	private Text usernameTxt;
	private Text passwordTxt;

	private Combo environmentCmb;
	private Label loadingLbl;
	private Label usernameLbl;
	private Label passwordLbl;
	
	private Button okBtn;
	private Button cancelBtn;
	
	private AtomicBoolean authenticated;

	public LoginSplash() {
		authenticated = new AtomicBoolean(false);
	}

	@Override
	public Shell getSplash() {
		return splash;
	}

	@Override
	public void init(final Shell splash) {
		this.splash = splash;
		super.init(getSplash());
		
		FillLayout layout = new FillLayout();
		getSplash().setLayout(layout);
		getSplash().setBackgroundMode(SWT.INHERIT_DEFAULT);
		
		createUI();
		getSplash().layout(true);
		doEventLoop();
	}

	private void doEventLoop() {
		Shell splash = getSplash();
		try {
			while (!authenticated.get()) {
				if (splash.getDisplay().readAndDispatch() == false) {
					splash.getDisplay().sleep();
				}
			}
		} catch (SWTException e) {
			if (e.code == SWT.ERROR_WIDGET_DISPOSED) {
				// Startup was aborted abnormally.
				System.exit(0);
			}
		}
		loginCmp.dispose();
		loadWorkbench();
		splash.layout(true);
	}

	private void createUI() {
		loginCmp = new Composite(getSplash(), SWT.NONE);
		GridLayout layout = new GridLayout(F_COLUMN_COUNT, false);
		layout.marginBottom = 20;
		loginCmp.setLayout(layout);
		
		Composite spanner = new Composite(loginCmp, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = F_COLUMN_COUNT;
		spanner.setLayoutData(data);
		
		Label versionLabel = new Label(loginCmp, SWT.NONE);
		versionLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		versionLabel.setText("Version: " + VersionUtils.getPhaedraVersion());
		GridDataFactory.fillDefaults().span(3, 1).hint(100, 80).indent(20, SWT.DEFAULT).applyTo(versionLabel);
		
		usernameLbl = new Label(loginCmp, SWT.NONE);
		usernameLbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		usernameLbl.setText("&User Name:"); // NON-NLS-1
		data = new GridData();
		data.horizontalIndent = F_LABEL_HORIZONTAL_INDENT;
		usernameLbl.setLayoutData(data);
		
		usernameTxt = new Text(loginCmp, SWT.BORDER);
		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_TEXT_WIDTH_HINT;
		data.horizontalSpan = 2;
		usernameTxt.setLayoutData(data);
		usernameTxt.setText(LDAPUtils.getAutoLoginName());
		
		passwordLbl = new Label(loginCmp, SWT.NONE);
		passwordLbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		passwordLbl.setText("&Password:"); // NON-NLS-1
		data = new GridData();
		data.horizontalIndent = F_LABEL_HORIZONTAL_INDENT;
		passwordLbl.setLayoutData(data);
		
		int style = SWT.PASSWORD | SWT.BORDER;
		passwordTxt = new Text(loginCmp, style);
		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_TEXT_WIDTH_HINT;
		data.horizontalSpan = 2;
		passwordTxt.setLayoutData(data);
		passwordTxt.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// allow both ENTER keys on the keyboard to log in directly
				if (e.keyCode == SWT.CR || e.keyCode == 16777296) {
					handleButtonOKWidgetSelected();
				}
			}
		});

		Label label = new Label(loginCmp, SWT.NONE);
		label.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		label.setText("&Environment:"); // NON-NLS-1
		data = new GridData();
		data.horizontalIndent = F_LABEL_HORIZONTAL_INDENT;
		label.setLayoutData(data);

		environmentCmb = new Combo(loginCmp, SWT.DROP_DOWN | SWT.READ_ONLY);
		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_TEXT_WIDTH_HINT - 16;
		data.horizontalSpan = 2;
		environmentCmb.setLayoutData(data);
		environmentCmb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleEnvironmentSelected();
			}
		});
		
		Label filler = new Label(loginCmp, SWT.NONE);
		filler.setVisible(false);
		loadingLbl = new Label(loginCmp, SWT.NONE);
		loadingLbl.setText("");
		data = new GridData();
		data.horizontalSpan = 2;
		data.widthHint = 300;
		loadingLbl.setLayoutData(data);
		loadingLbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		label = new Label(loginCmp, SWT.NONE);
		label.setVisible(false);
		
		okBtn = new Button(loginCmp, SWT.PUSH);
		okBtn.setText("Login"); // NON-NLS-1
		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_BUTTON_WIDTH_HINT;
		data.verticalIndent = 10;
		okBtn.setLayoutData(data);
		okBtn.setFocus();
		getSplash().setDefaultButton(okBtn);
		okBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleButtonOKWidgetSelected();
			}
		});
		
		cancelBtn = new Button(loginCmp, SWT.PUSH);
		cancelBtn.setText("Cancel"); // NON-NLS-1
		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = F_BUTTON_WIDTH_HINT;
		data.verticalIndent = 10;
		cancelBtn.setLayoutData(data);
		cancelBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleButtonCancelWidgetSelected();
			}
		});
		
		// Load environment configuration
		boolean retry = true;
		while (retry) {
			try {
				EnvironmentRegistry.getInstance().initialize();
				retry = false;
			} catch (Exception e) {
				EclipseLog.error("Failed to initialize environments", e, Activator.PLUGIN_ID);
				String cfg = ConfigLoader.getPreferredConfig();
				InputDialog dialog = new InputDialog(getSplash(), "Configuration",
					"No environment configuration was found. Please enter a valid configuration file path:", cfg, null);
				if (dialog.open() == Window.OK) ConfigLoader.setPreferredConfig(dialog.getValue());
				else retry = false;
			}
		}
		
		String[] env = EnvironmentRegistry.getInstance().getEnvironmentNames();
		if (env.length == 0) {
			AccessDialog.openLoginFailedDialog(getSplash(), "Configuration Error", "No environments are available."
					+ "\nPlease contact an administrator to adjust your environment configuration.");
			handleButtonCancelWidgetSelected();
		} else {
			environmentCmb.setItems(env);
			environmentCmb.select(0);
			handleEnvironmentSelected();
			passwordTxt.setFocus();
		}
	}

	private void handleEnvironmentSelected() {
		String envId = environmentCmb.getItem(environmentCmb.getSelectionIndex());
		IEnvironment env = EnvironmentRegistry.getInstance().getEnvironment(envId);
		boolean auth = env.requiresAuthentication();
		usernameLbl.setVisible(auth);
		usernameTxt.setVisible(auth);
		passwordLbl.setVisible(auth);
		passwordTxt.setVisible(auth);
	}

	private void handleButtonOKWidgetSelected() {
		String envId = environmentCmb.getItem(environmentCmb.getSelectionIndex());
		IEnvironment env = EnvironmentRegistry.getInstance().getEnvironment(envId);
		String username = usernameTxt.getText();
		String password = passwordTxt.getText();
		attemptLogin(env, username, password);
	}

	private void handleButtonCancelWidgetSelected() {
		EclipseLog.info("Login cancelled: " + new Date(), Activator.getDefault());
		Display.getDefault().close();
		System.exit(0);
	}

	private void attemptLogin(IEnvironment env, String username, String password) {
		loadingLbl.setText("Logging in...");
		okBtn.setEnabled(false);
		String envName = environmentCmb.getText();
		
		ForkJoinPool.commonPool().submit(() -> {
			try {
				Screening.login(env, username, password.getBytes());
				authenticated.set(true);
			} catch (AuthenticationException e) {
				EclipseLog.warn("Authentication failure", e, Activator.getDefault());
				loginFailed(e.getMessage(), false);
			} catch (Exception e) {
				String message = "An error occured connecting to the " + envName + " environment.";
				message += "\nPlease contact an administrator.";
				message += "\n\nCause:\n";
				if (e.getMessage() != null) message += e.getMessage();
				else if (e.getCause() != null) message += e.getCause().getMessage();
				else message += e.toString();
				
				EclipseLog.error("Error connecting to environment " + envName, e, Activator.getDefault());
				loginFailed(message, true);
			}
		});
	}
	
	private void loginFailed(String msg, boolean exit) {
		Display.getDefault().asyncExec(() -> {
			AccessDialog.openLoginFailedDialog(getSplash(), "Login failed", msg);
			passwordTxt.setText("");
			loadingLbl.setText("");
			passwordTxt.setFocus();
			okBtn.setEnabled(true);
			if (exit) handleButtonCancelWidgetSelected();
		});
	}
	
	private void loadWorkbench() {
		super.init(getSplash());
		
		String progressRectString = null;
		String messageRectString = null;
		String foregroundColorString = null;
		
		IProduct product = Platform.getProduct();
		if (product != null) {
			progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
			messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
			foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
		}
		
		setProgressRect(StringConverter.asRectangle(progressRectString, new Rectangle(10, 10, 300, 15)));
		setMessageRect(StringConverter.asRectangle(messageRectString, new Rectangle(10, 35, 300, 15)));

		int foregroundColorInteger = 0xD2D7FF;
		try { foregroundColorInteger = Integer.parseInt(foregroundColorString, 16); } catch (Exception ex) {}
		setForeground(new RGB((foregroundColorInteger & 0xFF0000) >> 16, (foregroundColorInteger & 0xFF00) >> 8, foregroundColorInteger & 0xFF));

		getContent();
	}
}
