package mn.aug.restfulandroid.rest.method;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mn.aug.restfulandroid.rest.DefaultRestClient;
import mn.aug.restfulandroid.rest.Request;
import mn.aug.restfulandroid.rest.Response;
import mn.aug.restfulandroid.rest.RestClient;
import mn.aug.restfulandroid.rest.resource.Login;
import mn.aug.restfulandroid.rest.resource.Resource;
import mn.aug.restfulandroid.security.LoginManager;
import mn.aug.restfulandroid.util.Logger;
import android.content.Context;

public abstract class AbstractRestMethod<T extends Resource> implements RestMethod<T> {

	private static final String DEFAULT_ENCODING = "UTF-8";
	private RestClient mRestClient;

	public RestMethodResult<T> execute() {

		Request request = buildRequest();
		if (requiresAuthorization()) {
			LoginManager loginManager = new LoginManager(getContext());
			authorize(request, loginManager.getLogin());
		}
		Response response = doRequest(request);
		return buildResult(response);
	}

	private void authorize(Request request, Login login) {

		if (request != null && login != null) {

			List<String> cookie = new ArrayList<String>();
			cookie.add("reddit_session=" + login.getCookie());
			request.addHeader("Cookie", cookie);

			String body = new String(request.getBody());
			body += "&uh=" + login.getModHash();
			request.setBody(body.getBytes());
		}
	}

	public void setRestClient(RestClient client) {
		mRestClient = client;
	}

	protected abstract Context getContext();

	/**
	 * Subclasses can overwrite for full control, eg. need to do special
	 * inspection of response headers, etc.
	 * 
	 * @param response
	 * @return
	 */
	protected RestMethodResult<T> buildResult(Response response) {

		int status = response.status;
		String statusMsg = "";
		String responseBody = null;
		T resource = null;

		try {
			responseBody = new String(response.body, getCharacterEncoding(response.headers));
			logResponse(status, responseBody);
			resource = parseResponseBody(responseBody);
		} catch (Exception ex) {
			// our own internal error code, not from service
			// spec only defines up to 505
			status = 506; 
			statusMsg = ex.getMessage();
		}
		return new RestMethodResult<T>(status, statusMsg, resource);
	}

	protected abstract URI getURI();

	protected String buildQueryString(Map<String, String> params) {

		StringBuilder queryStringBuilder = new StringBuilder();

		Iterator<String> paramKeyIter = params.keySet().iterator();

		if (paramKeyIter.hasNext()) {
			queryStringBuilder.append(getKeyValuePair(params, paramKeyIter.next()));
		}

		while (paramKeyIter.hasNext()) {
			queryStringBuilder.append("&" + getKeyValuePair(params, paramKeyIter.next()));
		}

		return queryStringBuilder.toString();
	}

	private String getKeyValuePair(Map<String, String> params, String key) {
		return key + "=" + params.get(key);
	}

	/**
	 * Returns the log tag for the class extending AbstractRestMethod
	 * 
	 * @return log tag
	 */
	protected abstract String getLogTag();

	/**
	 * Build the {@link Request}.
	 * 
	 * @return Request for this REST method
	 */
	protected abstract Request buildRequest();

	/**
	 * Determines whether the REST method requires authentication
	 * 
	 * @return <code>true</code> if authentication is required,
	 *         <code>false</code> otherwise
	 */
	protected boolean requiresAuthorization() {
		return true;
	}

	protected abstract T parseResponseBody(String responseBody) throws Exception;

	protected Response doRequest(Request request) {

		if (mRestClient == null) {
			mRestClient = new DefaultRestClient();
		}

		logRequest(request);
		return mRestClient.execute(request);
	}

	private String getCharacterEncoding(Map<String, List<String>> headers) {
		// TODO get value from headers
		return DEFAULT_ENCODING;
	}

	private void logRequest(Request request) {
		Logger.debug(getLogTag(), "Request: " + request.getMethod().toString() + " "
				+ request.getRequestUri().toASCIIString());
	}

	private void logResponse(int status, String responseBody) {
		Logger.debug(getLogTag(), "Response: status=" + status + ", body=" + responseBody);
	}

}
