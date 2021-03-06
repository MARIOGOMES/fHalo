/*
 * Copyright (c) 2010-2011 Mark Allen.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.facebook.halo.framework.core;

import static com.facebook.halo.framework.formatter.StringUtils.isBlank;
import static com.facebook.halo.framework.formatter.StringUtils.trimToEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.facebook.halo.framework.common.Parameter;
import com.facebook.halo.framework.exception.FacebookException;
import com.facebook.halo.framework.exception.FacebookExceptionMapper;
import com.facebook.halo.framework.exception.FacebookJsonMappingException;
import com.facebook.halo.framework.exception.FacebookOAuthException;
import com.facebook.halo.framework.exception.FacebookResponseStatusException;
import com.facebook.halo.framework.exception.JsonException;
import com.facebook.halo.framework.json.JsonMapper;
import com.facebook.halo.framework.json.JsonObject;


/**
 * Base class that contains data and functionality common to
 * {@link DefaultFacebookClient} and {@link DefaultLegacyFacebookClient}.
 * 
 * @author <a href="http://restfb.com">Mark Allen</a>
 * @since 1.5
 */

abstract public class BaseFacebookClient {

  /**
   * @param webRequestor : Handles {@code GET}s and {@code POST}s to the Facebook API endpoint.
   * @param jsonMapper : Handles mapping Facebook response JSON to Java objects.
   * @param legacyFacebookExceptionMapper   :  Knows how to map Old REST API exceptions to formal Java exception types.
   * @param illegalParamNames : Set of parameter names that user must not specify themselves, since we use these parameters internally.
   * @param readOnlyApiCalls : Set of API calls that can use the read-only endpoint for a performance boost.
   * @param logger : Logger
   * 
   */
  protected String accessToken;
  protected WebRequestor webRequestor;
  protected JsonMapper jsonMapper;
  protected FacebookExceptionMapper legacyFacebookExceptionMapper;
  protected final Set<String> illegalParamNames = new HashSet<String>();
  protected final Set<String> readOnlyApiCalls = new HashSet<String>();
  protected final Logger logger = Logger.getLogger(getClass().getName());
  
  /**
   * Initializes this Facebook client.
   */
  public BaseFacebookClient() {
    initializeReadOnlyApiCalls();
    legacyFacebookExceptionMapper = createLegacyFacebookExceptionMapper();
  }
  

  /**
   * Specifies how we map Old REST API exception types/messages to real Java
   * exceptions.
   * <p>
   * Uses an instance of {@link DefaultLegacyFacebookExceptionMapper} by
   * default.
   * 
   * @return An instance of the exception mapper we should use.
   * @since 1.6.3
   */
  protected FacebookExceptionMapper createLegacyFacebookExceptionMapper() {
    return new DefaultLegacyFacebookExceptionMapper();
  }

  /**
   * A canned implementation of {@code FacebookExceptionMapper} that maps Old
   * REST API exceptions.
   * 
   * @author <a href="http://restfb.com">Mark Allen</a>
   * @since 1.6.3
   */
  protected static class DefaultLegacyFacebookExceptionMapper implements FacebookExceptionMapper {

    /**
     * Invalid OAuth 2.0 Access Token error code.
     * <p>
     * See http://www.takwing.idv.hk/tech/fb_dev/faq/general/gen_10.html
     */
    protected static final int API_EC_PARAM_ACCESS_TOKEN = 190;

    /**
     * @see com.facebook.halo.framework.exception.FacebookExceptionMapper#exceptionForTypeAndMessage(java.lang.Integer,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public FacebookException exceptionForTypeAndMessage(Integer errorCode, String type, String message) {
      if (errorCode == API_EC_PARAM_ACCESS_TOKEN)
        return new FacebookOAuthException(String.valueOf(errorCode), message);

      // Don't recognize this exception type? Just go with the standard
      // FacebookResponseStatusException.
      return new FacebookResponseStatusException(errorCode, message);
    }
  }

  /**
   * Stores off the set of API calls that support the read-only endpoint.
   * <p>
   * This list was cribbed from the <a
   * href="https://github.com/facebook/php-sdk/blob/master/src/facebook.php"
   * target="_blank">Official PHP Facebook API client</a>.
   * 
   * @since 1.6.3
   */
  protected void initializeReadOnlyApiCalls() {
    readOnlyApiCalls.addAll(asList(new String[] { "admin.getallocation", "admin.getappproperties",
        "admin.getbannedusers", "admin.getlivestreamvialink", "admin.getmetrics", "admin.getrestrictioninfo",
        "application.getpublicinfo", "auth.getapppublickey", "auth.getsession", "auth.getsignedpublicsessiondata",
        "comments.get", "connect.getunconnectedfriendscount", "dashboard.getactivity", "dashboard.getcount",
        "dashboard.getglobalnews", "dashboard.getnews", "dashboard.multigetcount", "dashboard.multigetnews",
        "data.getcookies", "events.get", "events.getmembers", "fbml.getcustomtags", "feed.getappfriendstories",
        "feed.getregisteredtemplatebundlebyid", "feed.getregisteredtemplatebundles", "fql.multiquery", "fql.query",
        "friends.arefriends", "friends.get", "friends.getappusers", "friends.getlists", "friends.getmutualfriends",
        "gifts.get", "groups.get", "groups.getmembers", "intl.gettranslations", "links.get", "notes.get",
        "notifications.get", "pages.getinfo", "pages.isadmin", "pages.isappadded", "pages.isfan",
        "permissions.checkavailableapiaccess", "permissions.checkgrantedapiaccess", "photos.get", "photos.getalbums",
        "photos.gettags", "profile.getinfo", "profile.getinfooptions", "stream.get", "stream.getcomments",
        "stream.getfilters", "users.getinfo", "users.getloggedinuser", "users.getstandardinfo",
        "users.hasapppermission", "users.isappuser", "users.isverified", "video.getuploadlimits" }));
  }
  
  /**
   * If the {@code error_code} JSON field is present, we've got a response
   * status error for this API call. Extracts relevant information from the JSON
   * and throws an exception which encapsulates it for end-user consumption.
   * 
   * @param json
   *          The JSON returned by Facebook in response to an API call.
   * @throws FacebookResponseStatusException
   *           If the JSON contains an error code.
   * @throws FacebookJsonMappingException
   *           If an error occurs while processing the JSON.
   */
  protected void throwLegacyFacebookResponseStatusExceptionIfNecessary(String json) {
    try {
      // If this is not an object, it's not an error response.
      if (!json.startsWith("{"))
        return;

      JsonObject errorObject = null;

      // We need to swallow exceptions here because it's possible to get a legit
      // Facebook response that contains illegal JSON (e.g.
      // users.getLoggedInUser returning 1240077) - we're only interested in
      // whether or not there's an error_code field present.
      try {
        errorObject = new JsonObject(json);
      } catch (JsonException e) {}

      if (errorObject == null || !errorObject.has(DefaultFacebookString.LEGACY_ERROR_CODE_ATTRIBUTE_NAME))
        return;

      throw legacyFacebookExceptionMapper.exceptionForTypeAndMessage(
        errorObject.getInt(DefaultFacebookString.LEGACY_ERROR_CODE_ATTRIBUTE_NAME), null,
        errorObject.getString(DefaultFacebookString.LEGACY_ERROR_MSG_ATTRIBUTE_NAME));
    } catch (JsonException e) {
      throw new FacebookJsonMappingException("Unable to process the Facebook API response", e);
    }
  }
  
  /**
   * Appends the given {@code parameter} to the given {@code parameters} array.
   * 
   * @param parameter
   *          The parameter value to append.
   * @param parameters
   *          The parameters to which the given {@code parameter} is appended.
   * @return A new array which contains both {@code parameter} and
   *         {@code parameters}.
   */
  protected Parameter[] parametersWithAdditionalParameter(Parameter parameter, Parameter... parameters) {
    Parameter[] updatedParameters = new Parameter[parameters.length + 1];
    System.arraycopy(parameters, 0, updatedParameters, 0, parameters.length);
    updatedParameters[parameters.length] = parameter;
    return updatedParameters;
  }

  /**
   * Given a map of query names to queries, verify that it contains valid data
   * and convert it to a JSON object string.
   * 
   * @param queries
   *          The query map to convert.
   * @return The {@code queries} in JSON string format.
   * @throws IllegalArgumentException
   *           If the provided {@code queries} are invalid.
   */
  protected String queriesToJson(Map<String, String> queries) {
    DefaultFacebookUtils.verifyParameterPresence("queries", queries);

    if (queries.keySet().size() == 0)
      throw new IllegalArgumentException("You must specify at least one query.");

    JsonObject jsonObject = new JsonObject();

    for (Entry<String, String> entry : queries.entrySet()) {
      if (isBlank(entry.getKey()) || isBlank(entry.getValue()))
        throw new IllegalArgumentException("Provided queries must have non-blank keys and values. " + "You provided: "
            + queries);

      try {
        jsonObject.put(trimToEmpty(entry.getKey()), trimToEmpty(entry.getValue()));
      } catch (JsonException e) {
        // Shouldn't happen unless bizarre input is provided
        throw new IllegalArgumentException("Unable to convert " + queries + " to JSON.", e);
      }
    }

    return jsonObject.toString();
  }

//  protected String urlEncodedValueForParameterName(String name, String value) {
//    // Special handling for access_token -
//    // '%7C' is the pipe character and will be present in any access_token
//    // parameter that's already URL-encoded. If we see this combination, don't
//    // URL-encode. Otherwise, URL-encode as normal.
//    return DefaultFacebookString.ACCESS_TOKEN_PARAM_NAME.equals(name) && value.contains("%7C") ? value : urlEncode(value);
//  }

  /**
   * Given an api call (e.g. "me" or "fql.query"), returns the correct FB API
   * endpoint to use.
   * <p>
   * Useful for returning the read-only API endpoint where possible.
   * 
   * @param apiCall
   *          The FB API call (Graph or Old REST API) for which we'd like an
   *          endpoint.
   * @param hasAttachment
   *          Are we including a multipart file when making this API call?
   * @return An absolute endpoint URL to communicate with.
   * @since 1.6.3
   */
  protected String createEndpointForApiCall(String apiCall, boolean hasAttachment) {
		trimToEmpty(apiCall).toLowerCase();
		while (apiCall.startsWith("/"))
			apiCall = apiCall.substring(1);

		String baseUrl = getFacebookGraphEndpointUrl();

		if (readOnlyApiCalls.contains(apiCall)) baseUrl = getFacebookReadOnlyEndpointUrl();
		else if (hasAttachment && apiCall.endsWith("/videos")) baseUrl = getFacebookGraphVideoEndpointUrl();

		return format("%s/%s", baseUrl, apiCall);
	}

  /**
   * Returns the base read-only endpoint URL.
   * 
   * @return The base read-only endpoint URL.
   * @since 1.6.3
   */
  protected String getFacebookReadOnlyEndpointUrl() {
	 return DefaultFacebookString.FACEBOOK_READ_ONLY_ENDPOINT_URL;
  }
  
  /**
   * Returns the base endpoint URL for the Graph API's video upload functionality.
   * 
   * @return The base endpoint URL for the Graph API's video upload functionality.
   * @since 1.6.5
   */
  protected String getFacebookGraphVideoEndpointUrl() {
	 return DefaultFacebookString.FACEBOOK_GRAPH_VIDEO_ENDPOINT_URL;
  }
  
  /**
   * Returns the base endpoint URL for the Graph API.
   * 
   * @return The base endpoint URL for the Graph API.
   */
  protected String getFacebookGraphEndpointUrl() {
	return DefaultFacebookString.FACEBOOK_GRAPH_ENDPOINT_URL;
  }

  /**
   * Verifies that the provided parameter names don't collide with the ones we
   * internally pass along to Facebook.
   * 
   * @param parameters
   *          The parameters to check.
   * @throws IllegalArgumentException
   *           If there's a parameter name collision.
   */
  protected void verifyParameterLegality(Parameter... parameters) {
    for (Parameter parameter : parameters)
      if (illegalParamNames.contains(parameter.name))
        throw new IllegalArgumentException("Parameter '" + parameter.name + "' is reserved for RestFB use - "
            + "you cannot specify it yourself.");
  }
  
	  
  /**
   * Throws an exception if Facebook returned an error response. Using the Graph
   * API, it's possible to see both the new Graph API-style errors as well as
   * Legacy API-style errors, so we have to handle both here. This method
   * extracts relevant information from the error JSON and throws an exception
   * which encapsulates it for end-user consumption.
   * <p>
   * For Graph API errors:
   * <p>
   * If the {@code error} JSON field is present, we've got a response status
   * error for this API call.
   * <p>
   * For Legacy errors (e.g. FQL):
   * <p>
   * If the {@code error_code} JSON field is present, we've got a response
   * status error for this API call.
   * 
   * @param json
   *          The JSON returned by Facebook in response to an API call.
   * @throws FacebookGraphException
   *           If the JSON contains a Graph API error response.
   * @throws FacebookResponseStatusException
   *           If the JSON contains an Legacy API error response.
   * @throws FacebookJsonMappingException
   *           If an error occurs while processing the JSON.
   */
  protected void throwJsonFacebookResponseStatusExceptionIfNecessary(FacebookExceptionMapper graphFacebookExceptionMapper, String json) {
		// If we have a legacy exception, throw it.
	try {
		// If the result is not an object, bail immediately.
		if (!json.startsWith("{"))
			return;

		JsonObject errorObject = new JsonObject(json);

		if (errorObject == null || !errorObject.has(DefaultFacebookString.ERROR_ATTRIBUTE_NAME))
			return;

		JsonObject innerErrorObject = errorObject.getJsonObject(DefaultFacebookString.ERROR_ATTRIBUTE_NAME);

		throw graphFacebookExceptionMapper.exceptionForTypeAndMessage(null, innerErrorObject.getString(DefaultFacebookString.ERROR_TYPE_ATTRIBUTE_NAME),
						innerErrorObject.getString(DefaultFacebookString.ERROR_MESSAGE_ATTRIBUTE_NAME));
	} catch (JsonException e) {
		throw new FacebookJsonMappingException(
				"Unable to process the Facebook API response", e);
	}
  }

  /**
   * If the {@code error} and {@code error_description} JSON fields are present,
   * we've got a response status error for this batch API call. Extracts
   * relevant information from the JSON and throws an exception which
   * encapsulates it for end-user consumption.
   * 
   * @param json
   *          The JSON returned by Facebook in response to a batch API call.
   * @throws FacebookResponseStatusException
   *           If the JSON contains an error code.
   * @throws FacebookJsonMappingException
   *           If an error occurs while processing the JSON.
   * @since 1.6.5
   */
  protected void throwBatchFacebookResponseStatusExceptionIfNecessary(FacebookExceptionMapper legacyFacebookExceptionMapper, String json) {

	try {
		// If this is not an object, it's not an error response.
		if (!json.startsWith("{"))
			return;

		JsonObject errorObject = null;
		try {
			errorObject = new JsonObject(json);
		} catch (JsonException e) {
		}

		if (errorObject == null || !errorObject.has(DefaultFacebookString.BATCH_ERROR_ATTRIBUTE_NAME) || !errorObject.has(DefaultFacebookString.BATCH_ERROR_DESCRIPTION_ATTRIBUTE_NAME))
			return;

		throw legacyFacebookExceptionMapper.exceptionForTypeAndMessage(errorObject.getInt(DefaultFacebookString.BATCH_ERROR_ATTRIBUTE_NAME), null,
						errorObject.getString(DefaultFacebookString.BATCH_ERROR_DESCRIPTION_ATTRIBUTE_NAME));
	} catch (JsonException e) {
		throw new FacebookJsonMappingException("Unable to process the Facebook API response", e);
	}
  }

//  protected void verifyParameterPresence(String parameterName, String parameter) {
//    verifyParameterPresence(parameterName, (Object) parameter);
//    if (parameter.trim().length() == 0)
//      throw new IllegalArgumentException("The '" + parameterName + "' parameter cannot be an empty string.");
//  }
//
//  protected void verifyParameterPresence(String parameterName, Object parameter) {
//    if (parameter == null)
//      throw new NullPointerException("The '" + parameterName + "' parameter cannot be null.");
//  }
}