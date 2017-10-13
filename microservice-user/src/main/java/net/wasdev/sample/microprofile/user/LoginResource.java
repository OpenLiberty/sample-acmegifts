// ******************************************************************************
//  Copyright (c) 2017 IBM Corporation and others.
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  which accompanies this distribution, and is available at
//  http://www.eclipse.org/legal/epl-v10.html
//
//  Contributors:
//  IBM Corporation - initial API and implementation
// ******************************************************************************
package net.wasdev.sample.microprofile.user;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * This resource is responsible for logging in a user. Upon a successful login, the ID of the user
 * will be returned. Both regular login with username and password, as well as Twitter login, are
 * supported.
 */
@Path("/logins")
@RequestScoped
public class LoginResource {

  /** Access to user information stored in MongoDB. */
  @Inject private MongoAccess mongo;

  /**
   * The JWT of the current caller. Since this is a request scoped resource, the JWT will be
   * injected for each JAX-RS request. The injection is performed by the mpJwt-1.0 feature.
   */
  @Inject private JsonWebToken jwtPrincipal;

  /**
   * A location to store twitter oauth_secret used during the login process. Twitter requires a
   * secret to be obtained in phase one, and then used in phase two to sign the login request
   * parameters.
   *
   * <p>Note that this map does not work when this microservice is scaled out. In the event two of
   * these services are running, a separate caching service must be used.
   */
  private static final Map<String, RequestToken> twitterOauthLoginMap =
      new HashMap<String, RequestToken>();

  // -----------------------------------
  // Microprofile config injected values
  // -----------------------------------

  /**
   * The twitter consumer key for the twitter app representing this service. The consumer key is
   * used to log in the user via Twitter. This value is injected by MP Config. It is specified in
   * the project POM, and copied into the Liberty server bootstrap.properties during the build.
   * bootstrap.properties is a pre-defined config source for MP config.
   */
  @Inject
  @ConfigProperty(name = "twitter.consumer.key")
  private String twitterConsumerKey;

  /**
   * The twitter consumer secret for the twitter app representing this service. The consumer secret
   * is used to log in the user via Twitter. This value is injected by MP Config. It is specified in
   * the project POM, and copied into the Liberty server bootstrap.properties during the build.
   * bootstrap.properties is a pre-defined config source for MP config.
   */
  @Inject
  @ConfigProperty(name = "twitter.consumer.secret")
  private String twitterConsumerSecret;

  /**
   * The URL serving the twitter sign-in callback. Twitter requires a callback URL to be used after
   * Twitter receives the username and password from the user. The caller is then redirected to the
   * callback, where our application completes the sign-in process.
   *
   * <p>This value is injected by MP Config. It is specified in the project POM, and copied into the
   * Liberty server bootstrap.properties during the build. bootstrap.properties is a pre-defined
   * config source for MP config.
   */
  @Inject
  @ConfigProperty(name = "acme.gifts.frontend.url")
  private String frontEndCallbackURL;

  /** The user ID key for login response body */
  public static final String LOGIN_RESPONSE_ID_KEY = "id";

  /** The twitter key for login response body */
  public static final String LOGIN_RESPONSE_TWITTER_KEY = "twitter";

  /** Error key */
  public static final String LOGIN_RC_ERR_KEY = "error";

  /** Errors values to be returned to caller */
  public static final String LOGIN_RC_ERR_USR_NOT_FOUND = "userNotFound";

  public static final String LOGIN_RC_ERR_INCORRECT_PSWD = "incorrectPassword";
  public static final String LOGIN_RC_ERR_CANNOT_AUTH = "unableToAuthenticate";

  /**
   * This endpoint will attempt to log-in a user using the password specified in the user database.
   * The caller must have a valid JWT obtained from the auth service.
   *
   * @param payload The login request. The username and password must be specified.
   * @return The user ID if the login succeeded.
   */
  @POST
  @Path("/")
  @Consumes("application/json")
  public Response loginUser(JsonObject payload) {
    // Validate the JWT.  The JWT must be in the 'login' group to proceed.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    // Prepare a response.
    JsonBuilderFactory factory = Json.createBuilderFactory(null);
    JsonObjectBuilder builder = factory.createObjectBuilder();
    JsonObject responseBody = null;

    // Read the username and password from the request.
    String userName = payload.getString(User.JSON_KEY_USER_NAME);
    String password = payload.getString("password");

    // Lookup the userName in the database.  If they do not exist, return an error.
    DB database = mongo.getMongoDB();
    DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
    DBObject dbUser = dbCollection.findOne(new BasicDBObject(User.JSON_KEY_USER_NAME, userName));
    if (dbUser == null) {
      responseBody = builder.add(LOGIN_RC_ERR_KEY, LOGIN_RC_ERR_USR_NOT_FOUND).build();
      return Response.status(Status.BAD_REQUEST).entity(responseBody).build();
    }

    // Find out if this user was created in the usual way, or via a Twitter login.
    String dbId = ((ObjectId) dbUser.get(User.DB_ID)).toString();
    Boolean isTwitterUser = ((Boolean) dbUser.get(User.JSON_KEY_USER_TWITTER_LOGIN));
    boolean buildJwt = false;
    builder.add(LOGIN_RESPONSE_TWITTER_KEY, isTwitterUser);

    // If the user logs in the usual way, go ahead and check the password.  If they
    // log in via Twitter, return an error asking them to log in via Twitter.
    if (isTwitterUser == false) {
      // Check the password by pre-pending the cleartext password with the salt
      // for this user, and then hashing it.  Compare the hashed password with
      // the hashed password stored in the database.
      String passwordSaltString = ((String) dbUser.get(User.JSON_KEY_USER_PASSWORD_SALT));
      String correctHashedPassword = ((String) dbUser.get(User.JSON_KEY_USER_PASSWORD_HASH));

      try {
        PasswordUtility pwUtil = new PasswordUtility(password, passwordSaltString);
        if (correctHashedPassword.equals(pwUtil.getHashedPassword())) {
          responseBody = builder.add(LOGIN_RESPONSE_ID_KEY, dbId).build();
          buildJwt = true;
        } else {
          responseBody = builder.add(LOGIN_RC_ERR_KEY, LOGIN_RC_ERR_INCORRECT_PSWD).build();
          return Response.status(Status.UNAUTHORIZED).entity(responseBody).build();
        }
      } catch (Throwable t) {
        responseBody = builder.add(LOGIN_RC_ERR_KEY, LOGIN_RC_ERR_CANNOT_AUTH).build();
        return Response.serverError().entity(responseBody).build();
      }
    } else {
      responseBody = builder.add(LOGIN_RESPONSE_ID_KEY, "0").build();
    }

    // If the login succeeded, build the JWT that the caller should use on all future
    // calls.  The group name will be 'users' which gives the authenticated user access
    // to other services.
    ResponseBuilder okResponse = Response.ok(responseBody, MediaType.APPLICATION_JSON);
    if (buildJwt) {
      try {
        okResponse
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJWT(userName))
            .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION);
      } catch (Throwable t) {
        return Response.serverError().entity("Error building authorization token").build();
      }
    }

    return okResponse.build();
  }

  /**
   * This endpoint starts the Twitter login process. The Twitter login is a two-step process. During
   * the first step, the consumer key and secret used by this application is provided to Twitter,
   * and provides an oauth token and secret which is used in the second step. The token is returned
   * to the caller, the secret is stored for use during the second step.
   *
   * @return The oauth token that the user must provide to Twitter during the second step. The
   *     caller should provide the oauth token to Twitter during the login process, after which
   *     Twitter will redirect the user back to the second step of the login process.
   */
  @GET
  @Path("/twitter")
  public Response loginTwitterUser() {
    // Validate the JWT.  The JWT must be in the 'login' group to proceed.
    try {
      validateJWT();
    } catch (JWTException jwte) {
      return Response.status(Status.UNAUTHORIZED)
          .type(MediaType.TEXT_PLAIN)
          .entity(jwte.getMessage())
          .build();
    }

    JsonBuilderFactory factory = Json.createBuilderFactory(null);
    JsonObjectBuilder builder = factory.createObjectBuilder();

    // The first stage of authentication - use our Twitter consumer
    // key/secret to request a request token. Pass the request token
    // back in the response.
    try {
      // Validate Twitter config is set correctly and print an error
      // message if not.
      Configuration twitterConfig = buildTwitterConfiguration();
      if (twitterConfig == null) {
        JsonObject responseBody = builder.add(LOGIN_RC_ERR_KEY, "noTwitterConfig").build();
        return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
      }

      Twitter twitter = new TwitterFactory(twitterConfig).getInstance();
      RequestToken requestToken = twitter.getOAuthRequestToken(buildTwitterCallbackURL());
      String oauthToken = requestToken.getToken();

      // Store the oauth secret in the map so as not to expose it to the
      // client.
      twitterOauthLoginMap.put(oauthToken, requestToken);

      // Respond to the client with the oauth_token.
      JsonObject responseBody = builder.add("oauth_token", oauthToken).build();
      return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
    } catch (Throwable t) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.toString()).build();
    }
  }

  /**
   * This endpoint processes the second step of a Twitter login. At the conclusion of the first
   * step, the user was redirected back here with an oauth verifier. The oauth verifier is combined
   * with the oauth token provided in the first step, and the oauth secret provided in the first
   * step, to complete the login process.
   *
   * @param payload A JSON object containing the oauth token and oauth verifier.
   * @return The user ID of the logged in user.
   */
  @POST
  @Path("/twitter/verify")
  @Consumes("application/json")
  public Response verifyTwitterUser(JsonObject payload) {
    // No inbound-JWT check here, we assume the caller had one to get
    // its oauth secret into our login map.  The mpJwt-1.0 feature will
    // still enforce that there is some JWT present on the request.

    // Extract the oauth token and verifier from the request.
    String oauthToken = payload.getString("oauthToken");
    String oauthVerifier = payload.getString("oauthVerifier");

    try {
      // Retrieve the request secret from phase one of login and use it to
      // sign the validation message.  If everything was successful, then
      // Twitter will give us a user access token and we can complete the
      // signin process.
      Configuration twitterConfig = buildTwitterConfiguration();
      Twitter twitter = new TwitterFactory(twitterConfig).getInstance();
      RequestToken requestToken = twitterOauthLoginMap.remove(oauthToken);
      AccessToken userAccessToken = twitter.getOAuthAccessToken(requestToken, oauthVerifier);

      // Use the Twitter screen name as the user name. See if this user has a
      // record in our user database.
      String userName = userAccessToken.getScreenName();
      DB database = mongo.getMongoDB();
      DBCollection dbCollection = database.getCollection(User.DB_COLLECTION_NAME);
      DBObject dbUser = dbCollection.findOne(new BasicDBObject(User.JSON_KEY_USER_NAME, userName));

      JsonBuilderFactory factory = Json.createBuilderFactory(null);
      JsonObjectBuilder builder = factory.createObjectBuilder();
      JsonObject responseBody = null;
      boolean buildJwt = false;

      // If the user was not in our database, we need to create one.  Pre-populate
      // the user with information we received from Twitter.  The user can change
      // this later if they want to, using the user editor.
      if (dbUser == null) {
        twitter4j.User twitterUser = twitter.showUser(userName);
        User giftsUser = new User(twitterUser.getName(), userName);

        BasicDBObject dbEntry = giftsUser.getDBObject(false);
        dbCollection.insert(dbEntry);

        responseBody =
            builder
                .add(LOGIN_RESPONSE_ID_KEY, dbEntry.getString(User.DB_ID))
                .add(LOGIN_RESPONSE_TWITTER_KEY, true)
                .build();
        buildJwt = true;
      } else {
        // If user is already in the database, make sure they are a twitter
        // user. If not, we have a security hole.
        boolean isTwitterLogin = (Boolean) dbUser.get(User.JSON_KEY_USER_TWITTER_LOGIN);
        if (isTwitterLogin) {
          builder.add(LOGIN_RESPONSE_ID_KEY, ((ObjectId) dbUser.get(User.DB_ID)).toString());
          buildJwt = true;
        } else {
          builder.add(LOGIN_RESPONSE_ID_KEY, "0");
        }
        responseBody = builder.add(LOGIN_RESPONSE_TWITTER_KEY, isTwitterLogin).build();
      }

      // Create a JWT. Note that we are not saving the Twitter access token because
      // we will never use it again. OK that's probably a lie - we may use it again
      // someday and at that point we'll need to figure out how to save it.
      ResponseBuilder okResponse = Response.ok(responseBody, MediaType.APPLICATION_JSON);
      if (buildJwt) {
        try {
          okResponse
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJWT(userName))
              .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION);
        } catch (Throwable t) {
          return Response.serverError().entity("Erorr building authorization token").build();
        }
      }

      return okResponse.build();

    } catch (Throwable t) {
      return Response.serverError().entity(t.toString()).build();
    }
  }

  /**
   * Build the URL that Twitter will redirect the user to, after the first phase of the login
   * process. The redirect is to the front end, so we build the URL here using the hostname and port
   * that the front end is using.
   *
   * @return The URL that Twitter will redirect us to after the first phase of the login.
   */
  private final String buildTwitterCallbackURL() {
    return frontEndCallbackURL + "/login/twitter/verify";
  }

  /**
   * Build the configuration that twitter4j will use.
   *
   * @return
   */
  private final Configuration buildTwitterConfiguration() {
    if ((twitterConsumerKey == null)
        || (twitterConsumerSecret == null)
        || (twitterConsumerKey.equals("CHANGE_ME"))
        || (twitterConsumerSecret.equals("CHANGE_ME"))) {
      return null;
    }

    return new ConfigurationBuilder()
        .setOAuthConsumerKey(twitterConsumerKey)
        .setOAuthConsumerSecret(twitterConsumerSecret)
        .build();
  }

  /**
   * Build a JWT that will be used by an authenticated user. The JWT wil be in the 'users' group and
   * should contain the username as defined by MP JWT.
   *
   * @param userName The name of the authenticated user.
   * @return A compact JWT that should be returned to the caller.
   * @throws Exception Something went wrong...?
   */
  private String buildJWT(String userName) throws Exception {
    return JwtBuilder.create("jwtUserBuilder")
        .claim(Claims.SUBJECT, userName)
        .claim("upn", userName) /* MP-JWT defined subject claim */
        .claim("groups", "users") /* MP-JWT builds an array from this */
        .buildJwt()
        .compact();
  }

  /** Do some basic checks on the JWT, until the MP-JWT annotations are ready. */
  private void validateJWT() throws JWTException {
    // Make sure the authorization header was present.  This check is somewhat
    // silly since the jwtPrincipal will never actually be null since it's a
    // WELD proxy (injected).
    if (jwtPrincipal == null) {
      throw new JWTException("No authorization header or unable to inflate JWT");
    }

    // Make sure we're in the login group.  This JWT only allows logins, after
    // which a new JWT will be created for the specific authenticated user.
    Set<String> groups = jwtPrincipal.getGroups();
    if ((groups == null) || (groups.contains("login") == false)) {
      throw new JWTException("User is not in a valid group [" + groups.toString() + "]");
    }

    // TODO: Additional checks as appropriate.
  }

  private static class JWTException extends Exception {
    private static final long serialVersionUID = 423763L;

    public JWTException(String message) {
      super(message);
    }
  }
}
