package eu.openanalytics.phaedra.base.environment.login;

import java.io.IOException;
import java.util.Date;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.StringConverter;
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
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.ui.AccessDialog;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;

public class LoginSplash extends BasicSplashHandler {

	private static final int MAX_ATTEMPTS = 3;
	
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
	
	private boolean authenticated;
	private int attempts = 0;

	public LoginSplash() {
		loginCmp = null;
		usernameTxt = null;
		passwordTxt = null;
		okBtn = null;
		cancelBtn = null;
		authenticated = false;
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
		
		// Keep the splash screen visible and prevent the RCP application from
		// loading until the close button is clicked.
		doEventLoop();
	}

	private void doEventLoop() {
		Shell splash = getSplash();
		try {
			while (authenticated == false) {
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
		loading();
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
		environmentCmb.setItems(EnvironmentRegistry.getInstance().getEnvironmentNames());
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
		
		String username = System.getProperty("user.name");
		String domain = System.getenv("USERDOMAIN");
		if (username != null) {
			if (domain != null) username = domain + "\\" + username;
			usernameTxt.setText(username);
		}
		
		if (environmentCmb.getItems().length > 0) environmentCmb.select(0);
		handleEnvironmentSelected();
		passwordTxt.setFocus();
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
		
		if (attempts < MAX_ATTEMPTS) {
			try {
				loadingLbl.setText("Logging in...");
				String username = usernameTxt.getText();
				byte[] password = passwordTxt.getText().getBytes();
				Screening.login(env, username, password);
				authenticated = true;
			} catch (AuthenticationException e) {
				AccessDialog.openLoginFailedDialog(getSplash(), "Login failed", e.getMessage());
				attempts++;
				passwordTxt.setText("");
				loadingLbl.setText("");
				passwordTxt.setFocus();
			} catch (IOException e) {
				String message = "An error occured connecting to the " + environmentCmb.getText() + " environment.";
				message += "\nPlease contact an administrator.";
				message += "\n\nCause:\n";
				if (e.getMessage() != null) message += e.getMessage();
				else if (e.getCause() != null) message += e.getCause().getMessage();
				else message += e.toString();
				
				EclipseLog.error("Error connecting to environment " + environmentCmb.getText(), e, Activator.getDefault());
				AccessDialog.openLoginFailedDialog(getSplash(), "Connection Error", message);
				handleButtonCancelWidgetSelected();
			}
		}

		if (!authenticated && attempts >= MAX_ATTEMPTS) {
			handleButtonCancelWidgetSelected();
		}
	}

	private void handleButtonCancelWidgetSelected() {
		EclipseLog.info("Session cancelled: " + new Date() + " (before login)", Activator.getDefault());
		Display.getDefault().close();
		System.exit(0);
	}

	private void loading() {
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
		Rectangle progressRect = StringConverter.asRectangle(progressRectString, new Rectangle(10, 10, 300, 15));
		setProgressRect(progressRect);

		Rectangle messageRect = StringConverter.asRectangle(messageRectString, new Rectangle(10, 35, 300, 15));
		setMessageRect(messageRect);

		int foregroundColorInteger;
		try {
			foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
		} catch (Exception ex) {
			foregroundColorInteger = 0xD2D7FF; // off white
		}

		setForeground(new RGB((foregroundColorInteger & 0xFF0000) >> 16, (foregroundColorInteger & 0xFF00) >> 8, foregroundColorInteger & 0xFF));

		getContent(); // ensure creation of the progress
	}
}
