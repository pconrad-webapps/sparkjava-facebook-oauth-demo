package org.pconrad.webapps.sparkjava;

import java.util.HashMap;
import java.util.Map;
import static java.util.Map.Entry;

import spark.ModelAndView;

import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.before;

import spark.Request;
import spark.Response;

import org.pac4j.core.config.Config;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.ApplicationLogoutRoute;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;

import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.facebook.FacebookPicture;

import org.pac4j.oauth.profile.OAuth20Profile;

import java.util.Collection;


/**
   Demo of Spark Pac4j with Facebook OAuth

   @author pconrad
 */
public class SparkFacebookOAuthDemo {

    private static String FACEBOOK_CLIENT_ID;
    
    private static java.util.List<CommonProfile> getProfiles(final Request request,
						   final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager manager = new ProfileManager(context);
	return manager.getAll(true);
    }    
    
    private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

    /** 
	add facebook information to the session

    */
    private static Map addFacebook(Map model, Request request, Response response) {
	FacebookProfile fp = ((FacebookProfile)(model.get("fp")));
	if (fp == null) {
	    System.out.println("model contains no facebook profile fp");
	    return model;
	}
	try {

	    // Here is where you can try getting an OAuth Token and using it
	    // to access the Facebook API
	    
	    OAuth20Profile oa2p = ( OAuth20Profile ) model.get("firstProfile");
	    if (oa2p==null) {
		System.out.println("model contains Facebook profile fp, but no firstProfile");
		return model;
	    }
	    String accessToken = oa2p.getAccessToken();
	    System.err.println("accessToken="+accessToken);
	    if (accessToken != null) {

	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return model;
    }
    
    private static Map buildModel(Request request, Response response) {

	final Map model = new HashMap<String,Object>();

	// TODO: Refactor this... it's a global variable, and that's icky
	
	if (FACEBOOK_CLIENT_ID==null) {
	    model.put("facebook_client_id","");
	} else {
	    model.put("facebook_client_id",FACEBOOK_CLIENT_ID);
	}
	
	Map<String, Object> map = new HashMap<String, Object>();
	for (String k: request.session().attributes()) {
	    Object v = request.session().attribute(k);
	    map.put(k,v);
	}
	
	model.put("session", map.entrySet());
	
	java.util.List<CommonProfile> userProfiles = getProfiles(request,response);

	model.put("profiles", userProfiles);

	try {
	    if (userProfiles.size()>0) {
		CommonProfile firstProfile = userProfiles.get(0);
		model.put("firstProfile", firstProfile);	
		
		FacebookProfile fp = (FacebookProfile) firstProfile;
		// See: https://github.com/pac4j/pac4j/blob/master/pac4j-oauth/src/main/java/org/pac4j/oauth/profile/facebook/FacebookProfile.java

		// And: https://github.com/pac4j/pac4j/blob/master/pac4j-oauth/src/main/java/org/pac4j/oauth/profile/facebook/FacebookPicture.java

		FacebookPicture fbPic = fp.getPicture();
		
		model.put("fp", fp); 
		model.put("name",fp.getDisplayName());
		if (fbPic!=null)
		    model.put("avatar_url",fbPic.getUrl());
		else
		    model.put("avatar_url","");
		model.put("email",fp.getEmail()); 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	
	return model;	
    }

    /**

       return a HashMap with values of all the environment variables
       listed; print error message for each missing one, and exit if any
       of them is not defined.
    */
    
    public static HashMap<String,String> getNeededEnvVars(String [] neededEnvVars) {
	HashMap<String,String> envVars = new HashMap<String,String>();
	
	
	for (String k:neededEnvVars) {
	    String v = System.getenv(k);
	    envVars.put(k,v);
	}
	
	boolean error=false;
	for (String k:neededEnvVars) {
	    if (envVars.get(k)==null) {
		error = true;
		System.err.println("Error: Must define env variable " + k);
	    }
	}
	if (error) { System.exit(1); }
	
	return envVars;
    }
    
    public static void main(String[] args) {
	
	HashMap<String,String> envVars =
	    getNeededEnvVars(new String []{ "FACEBOOK_CLIENT_ID",
					    "FACEBOOK_CLIENT_SECRET",
					    "FACEBOOK_CALLBACK_URL",
					    "APPLICATION_SALT"});

	FACEBOOK_CLIENT_ID=envVars.get("FACEBOOK_CLIENT_ID");
	
	Spark.staticFileLocation("/static");
	
	try {
	    // needed for Heroku
	    Spark.port(Integer.valueOf(System.getenv("PORT"))); 
	} catch (Exception e) {
	    System.err.println("NOTICE: using default port." +
			       " Define PORT env variable to override");
	}

	Config config = new
	    FacebookOAuthConfigFactory(envVars.get("FACEBOOK_CLIENT_ID"),
				     envVars.get("FACEBOOK_CLIENT_SECRET"),
				     envVars.get("FACEBOOK_CALLBACK_URL"),
				     envVars.get("APPLICATION_SALT"),
				     templateEngine).build();

	final SecurityFilter
	    facebookFilter = new SecurityFilter(config, "FacebookClient", "", "");

	get("/",
	    (request, response) -> new ModelAndView(buildModel(request,response),"home.mustache"),
	    templateEngine);

	before("/login", facebookFilter);

	get("/login",
	    (request, response) -> new ModelAndView(buildModel(request,response),"home.mustache"),
	    templateEngine);

	get("/logout", new ApplicationLogoutRoute(config, "/"));
	
	get("/session",
	    (request, response) -> new ModelAndView(buildModel(request,response),
						    "session.mustache"),
	    templateEngine);

	get("/facebook",
	    (request, response) ->
	    new ModelAndView(addFacebook(buildModel(request,response),request,response),
			     "facebook.mustache"),
	    templateEngine);

	
	final org.pac4j.sparkjava.CallbackRoute callback =
	    new org.pac4j.sparkjava.CallbackRoute(config);

	get("/callback", callback);
	post("/callback", callback);

    }
}
