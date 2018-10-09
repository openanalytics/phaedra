package eu.openanalytics.phaedra.base.security.windows;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.LastErrorException;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.ILoginHandler;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Roles;

public class WindowsLoginHandler implements ILoginHandler {

	private AuthConfig authConfig;
	
	public WindowsLoginHandler(AuthConfig authConfig) {
		this.authConfig = authConfig;
	}
	
	@Override
	public void authenticate(String userName, byte[] password) throws AuthenticationException {
		HANDLEByReference phUser = new HANDLEByReference();
		try {
			boolean loginOk = Advapi32.INSTANCE.LogonUser(userName, InetAddress.getLocalHost().getHostName(),
					new String(password), WinBase.LOGON32_LOGON_NETWORK, WinBase.LOGON32_PROVIDER_DEFAULT, phUser);
			if (!loginOk) throw new LastErrorException(Kernel32.INSTANCE.GetLastError());
			
			SecurityService.getInstance().setCurrentUser(userName);
			Map<Group, List<String>> groups = new HashMap<Group, List<String>>();
			String role = authConfig.get(AuthConfig.GLOBAL_ROLE);
			role = (role == null) ? Roles.USER : role.toUpperCase();
			groups.put(new Group(Group.GLOBAL_TEAM, role), Collections.singletonList(SecurityService.getInstance().getCurrentUserName()));
			SecurityService.getInstance().setSecurityConfig(groups);
		} catch (Exception e) {
			throw new AuthenticationException("Windows authentication failed", e);
		}
	}

}
