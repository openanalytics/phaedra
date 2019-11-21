package eu.openanalytics.phaedra.base.internal.security.oidc;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Response;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import eu.openanalytics.phaedra.base.internal.security.oidc.OidcHttpService.HttpResponseHandler;
import eu.openanalytics.phaedra.base.security.AbstractLoginHandler;
import eu.openanalytics.phaedra.base.security.Activator;
import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Roles;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;


/**
 * Login handler using OpenID Connect (OIDC)
 */
public class OidcLoginHandler extends AbstractLoginHandler {
	
	
	private static class Config {
		
		private Issuer discoveryIssuer;
		private OIDCClientInformation clientInfo;
		
		private Scope scope;
		private final String usernameClaim;
		private final String groupClaim;
		
		private final int connectionTimeoutMillis = 30_000;
		
		public Config(final AuthConfig authConfig) {
			{	final String uri = getRequired(authConfig, "discovery.uri");
				this.discoveryIssuer = new Issuer(uri);
			}
			{	final String id = getRequired(authConfig, "client.id");
				final ClientID clientId = new ClientID(id);
				
				final OIDCClientMetadata clientMetadata = new OIDCClientMetadata();
				clientMetadata.setApplicationType(ApplicationType.NATIVE);
				
				final String idTokenJWSAlgKey = get(authConfig, "id.token.signed.response.alg");
				clientMetadata.setIDTokenJWSAlg((idTokenJWSAlgKey != null) ?
						JWSAlgorithm.parse(idTokenJWSAlgKey) :
						JWSAlgorithm.RS256); // default (spec core-1.0 3.1.3.7.)
				this.clientInfo = new OIDCClientInformation(clientId, null, clientMetadata, null);
			}
			
			{	final Scope scope = new Scope("openid");
				final String additional = get(authConfig, "scopes");
				if (additional != null && !additional.isEmpty()) {
					scope.addAll(Scope.parse(additional));
				}
				this.scope = scope;
			}
			this.usernameClaim = getRequired(authConfig, "username.claim");
			this.groupClaim = getRequired(authConfig, "groups.claim");
		}
		
		
		private static String get(final AuthConfig authConfig, final String key) {
			String value = authConfig.get(key);
			if (value != null && !value.isEmpty()) return value;
			value = authConfig.get(key.replace('.', '_')); // also accept spec syntax
			if (value != null && !value.isEmpty()) return value;
			return null;
		}
		
		private static String getRequired(final AuthConfig authConfig, final String key) {
			final String value = get(authConfig, key);
			if (value == null) {
				throw new IllegalArgumentException(String.format("'%1$s' for OIDC is missing.", key)); 
			}
			return value;
		}
		
	}
	
	
	private Config config;
	private IDTokenValidator idTokenValidator;
	
	private OIDCProviderMetadata providerMetadata;
	
	private Nonce nonce;
	private URI redirectUri;
	
//	private JWT idToken;
	private IDTokenClaimsSet idTokenClaimsSet;
	
	
	public OidcLoginHandler(final SecurityService securityService, final AuthConfig authConfig) {
		super(securityService, authConfig);
	}
	
	
	@Override
	public Collection<String> getRequiredParameter() {
		return Collections.emptyList();
	}
	
	@Override
	public void authenticate(final String userName, final byte[] password,
			final boolean setUserContext) throws AuthenticationException {
		try {
			this.config = new Config(getAuthConfig());
			updateProviderMetadata();
			
			final AuthorizationCode authorizationCode = authenticate();
			final BearerAccessToken accessTokens = requestToken(new AuthorizationCodeGrant(authorizationCode, this.redirectUri));
			
			if (setUserContext) {
				final UserInfo userInfo = loadUserInfo(accessTokens);
				applyUserInfo(userInfo);
			}
		}
		catch (final IOException e) {
			throw newAuthException("Authentication failed because of an error.", e);
		}
	}
	
	protected void updateProviderMetadata() throws AuthenticationException {
		try {
			final OIDCProviderMetadata metadata = OIDCProviderMetadata.resolve(this.config.discoveryIssuer,
					this.config.connectionTimeoutMillis, 15_000 );
			
			assertEquals("issuer (issuer)", this.config.discoveryIssuer, metadata.getIssuer()); //$NON-NLS-1$
			this.providerMetadata = metadata;
		}
		catch (final GeneralException | IOException | BadJWTException e) {
			throw newAuthException("discovering the OIDC provider failed.", e);
		}
		
		try {
			this.idTokenValidator = (this.config.clientInfo.getOIDCMetadata().getIDTokenJWSAlg() != null) ?
					IDTokenValidator.create(this.providerMetadata, this.config.clientInfo) :
					new IDTokenValidator(this.providerMetadata.getIssuer(), this.config.clientInfo.getID());
		}
		catch (final GeneralException e) {
			throw newAuthException("invalid OIDC client configuration.", e);
		}
	}
	
	private static class AuthResponseHandler implements HttpResponseHandler {
		
		private static final int CANCELED = -1;
		private static final int OK = 1;
		
		private final State state;
		
		private int status;
		
		private AuthenticationResponse response;
		
		public AuthResponseHandler(final State state) {
			this.state = state;
		}
		
		private void setStatus(final int status) {
			this.status = status;
			notifyAll();
		}
		
		@Override
		public synchronized boolean handle(final HTTPRequest httpResponse) {
			if (this.status != 0) {
				return false;
			}
			try {
				final AuthenticationResponse authResponse = AuthenticationResponseParser.parse(httpResponse);
				if (!this.state.equals(authResponse.getState())) {
					Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
							"OIDC authentication response ignored because of invalid state." ));
					return false;
				}
				
				this.response = authResponse;
				setStatus(OK);
				return true;
			}
			catch (final Exception e) {
				Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"OIDC authentication response ignore because of unknown .", e ));
				return false;
			}
		}
		@Override
		public synchronized void cancel() {
			if (this.status != 0) {
				return;
			}
			setStatus(CANCELED);
		}
		
		public synchronized AuthenticationResponse getResponse() throws AuthenticationException {
			while (this.status == 0) {
				try {
					wait();
				}
				catch (final InterruptedException e) {
					throw newAuthCancelledException();
				}
			}
			if (this.status == CANCELED) {
				throw newAuthCancelledException();
			}
			return this.response;
		}
	}
	
	private AuthorizationCode authenticate() throws AuthenticationException, IOException {
		final OidcHttpService httpService = OidcHttpService.getHttpService();
		
		final State state = new State();
		final Nonce nonce = new Nonce();
		
		final AuthResponseHandler responseHandler = new AuthResponseHandler(state);
		final URI redirectUri = httpService.startAuthExchange(responseHandler);
		try {
			final AuthenticationRequest request = new AuthenticationRequest.Builder(
							new ResponseType(ResponseType.Value.CODE),
							this.config.scope,
							this.config.clientInfo.getID(),
							redirectUri )
					.endpointURI(this.providerMetadata.getAuthorizationEndpointURI())
					.state(state)
					.nonce(nonce)
					.loginHint(System.getProperty("pheadra.login.username.hint"))
					.build();
			
			final URI uri = request.toURI();
			Desktop.getDesktop().browse(uri);
			
			final AuthenticationResponse response = responseHandler.getResponse();
			assertEquals("state (state)", state, response.getState()); //$NON-NLS-1$
			checkResponse(response);
			final AuthenticationSuccessResponse successResponse = response.toSuccessResponse();
			
			this.nonce = nonce;
			this.redirectUri = redirectUri;
			return successResponse.getAuthorizationCode();
		}
		catch (final GeneralException | IOException | BadJWTException e) {
			throw newAuthException("receiving the OIDC authorization code failed.", e);
		}
		finally {
			httpService.endAuthReponse(responseHandler);
		}
	}
	
	private BearerAccessToken requestToken(final AuthorizationGrant authorizationGrant) throws AuthenticationException {
		try {
			final TokenRequest request = new TokenRequest(this.providerMetadata.getTokenEndpointURI(),
					this.config.clientInfo.getID(),
					authorizationGrant);
			
			final HTTPResponse httpResponse = request.toHTTPRequest().send();
			
			final TokenResponse response = OIDCTokenResponseParser.parse(httpResponse);
			checkResponse(response);
			final OIDCTokenResponse accessTokenResponse = (OIDCTokenResponse)response.toSuccessResponse();
			
			final OIDCTokens oidcTokens = accessTokenResponse.getOIDCTokens();
			final JWT idToken = oidcTokens.getIDToken();
			final IDTokenClaimsSet idTokenClaimsSet = validateIdToken(idToken);
			
//			this.idToken = idToken;
			this.idTokenClaimsSet = idTokenClaimsSet;
			return oidcTokens.getBearerAccessToken();
		}
		catch (final GeneralException | IOException e) {
			throw newAuthException("receiving the OIDC access token failed.", e);
		}
	}
	
	private IDTokenClaimsSet validateIdToken(final JWT token) {
		try {
			return this.idTokenValidator.validate(token, this.nonce);
		}
		catch (final BadJOSEException | JOSEException e) {
			throw newAuthException("the received OIDC id token is invalid.", e);
		}
	}
	
	private UserInfo loadUserInfo(final BearerAccessToken accessToken) throws AuthenticationException {
		try {
			final UserInfoRequest request = new UserInfoRequest(this.providerMetadata.getUserInfoEndpointURI(),
					accessToken );
			
			final HTTPResponse httpResponse = request.toHTTPRequest().send();
			
			final UserInfoResponse response = UserInfoResponse.parse(httpResponse);
			checkResponse(response);
			final UserInfoSuccessResponse successResponse = response.toSuccessResponse();
			
			final UserInfo userInfo = successResponse.getUserInfo();
			return validateUserInfo(userInfo);
		}
		catch (final GeneralException | IOException e) {
			throw newAuthException("loading user information failed.", e);
		}
	}
	
	private UserInfo validateUserInfo(final UserInfo userInfo) throws AuthenticationException {
		try {
			assertEquals("subject (sub)", this.idTokenClaimsSet.getSubject(), userInfo.getSubject()); //$NON-NLS-1$
			return userInfo;
		}
		catch (final BadJWTException e) {
			throw newAuthException("the received user information is invalid.", e);
		}
	}
	
	private void applyUserInfo(final UserInfo userInfo) throws AuthenticationException {
		try {
			final JSONObject jsonClaimSet = userInfo.toJSONObject();
			final String username = getRequiredStringClaim(jsonClaimSet, this.config.usernameClaim, "username");
			final Object object = jsonClaimSet.get(this.config.groupClaim);
			
			final SecurityService securityService = getSecurityService();
			securityService.setCurrentUser(username);
			System.out.println("group.claim (" + this.config.groupClaim + ") = " + object);
			
			// TODO
			final Map<Group, List<String>> groups = new HashMap<Group, List<String>>();
			String role = getAuthConfig().get(AuthConfig.GLOBAL_ROLE);
			role = (role == null) ? Roles.USER : role.toUpperCase();
			groups.put(new Group(Group.GLOBAL_TEAM, role), Collections.singletonList(securityService.getCurrentUserName()));
			securityService.setSecurityConfig(groups);
		}
		catch (final GeneralException e) {
			throw newAuthException("the received user information does not contain the required data.", e);
		}
	}
	
	private static String getRequiredStringClaim(final JSONObject jsonClaimSet, final String key, final String label) throws ParseException {
		final Object value = jsonClaimSet.get(key);
		if (value == null) {
			throw new ParseException("claim '%1$s' for %2$s is missing.");
		}
		if (value instanceof String) {
			final String s = (String) value;
			if (s.isEmpty()) {
				throw new ParseException("claim '%1$s' for %2$s is empty.");
			}
			return s;
		}
		throw new ParseException("claim '%1$s' for %2$s is invalid: unexpected type.");
	}
	
	
	private static void assertEquals(final String key, final Object expected, final Object actual) throws BadJWTException {
		if (!expected.equals(actual)) {
			throw new BadJWTException(String.format("Unexpected JWT %1$s: %2$s", key, actual));
		}
	}
	
	private static class ErrorResponseException extends GeneralException {
		
		private static final long serialVersionUID = 1L;
		
		public ErrorResponseException(final String message, final ErrorObject error) {
			super(message, error);
		}
		
	}
	
	private static void checkResponse(final Response response) throws ErrorResponseException {
		if (!response.indicatesSuccess()) {
			throw new ErrorResponseException("Error response received.", ((ErrorResponse) response).getErrorObject());
		}
	}
	
	private static AuthenticationException newAuthException(final String message, final Throwable cause) {
		final StringBuilder authMessage = new StringBuilder("Authentication failed: ");
		authMessage.append(message);
		if (cause instanceof GeneralException) {
			final ErrorObject errorObject = ((GeneralException)cause).getErrorObject();
			
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					authMessage.toString() + "\n" + errorObject.toJSONObject().toJSONString(JSONStyle.NO_COMPRESS),
					cause ));
			
			if (cause instanceof ErrorResponseException
					&& (errorObject.getCode() != null || errorObject.getHTTPStatusCode() != 0) ) {
				authMessage.append('\n');
				if (errorObject.getCode() != null) {
					authMessage.append('\n');
					authMessage.append("Reason: OIDC error ");
					authMessage.append(errorObject.getCode());
				}
				else if (errorObject.getHTTPStatusCode() != 0) {
					authMessage.append('\n');
					authMessage.append("Reason: HTTP error ");
					authMessage.append(errorObject.getCode());
				}
				if (errorObject.getDescription() != null && !errorObject.getDescription().isEmpty()) {
					authMessage.append(" - "); //$NON-NLS-1$
					authMessage.append(errorObject.getDescription());
				}
				if (errorObject.getURI() != null) {
					authMessage.append('\n');
					authMessage.append("See also: ");
					authMessage.append(errorObject.getURI().toString());
				}
			}
		}
		else {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					authMessage.toString(),
					cause ));
		}
		throw new AuthenticationException(authMessage.toString());
	}
	
	private static AuthenticationException newAuthCancelledException() {
		throw new AuthenticationException("Authentication cancelled.");
	}
	
}
