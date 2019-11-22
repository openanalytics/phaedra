package eu.openanalytics.phaedra.base.internal.security.oidc;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.ServletUtils;


public class AuthServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	
	
	public AuthServlet() {
	}
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (OidcHttpService.AUTH_RESPONSE_PATH.equals(req.getServletPath() + req.getPathInfo())) {
			final HTTPRequest oidcRequest = ServletUtils.createHTTPRequest(req);
			final OidcHttpService httpService = OidcHttpService.getHttpService();
			if (httpService.handleAuthReponse(oidcRequest)) {
				writeResponse(resp);
				return;
			}
			else {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
		
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		return;
	}
	
	protected void writeResponse(final HttpServletResponse resp) throws IOException {
		final PrintWriter writer = resp.getWriter();
		
		resp.setContentType("text/html;charset=UTF-8"); //$NON-NLS-1$
		resp.setHeader("Cache-Control", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
		
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"); //$NON-NLS-1$
		writer.println("<html><head>"); //$NON-NLS-1$
		writer.println("<title>Phaedra Login</title>"); //$NON-NLS-1$
		writer.println("</head><body>"); //$NON-NLS-1$
		writer.println("You can close this browser tab. Continue in the Phaedra application.");
		writer.println("</body></html>"); //$NON-NLS-1$
	}
	
}
