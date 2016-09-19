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


import java.util.Collection;


/**
   Demo of Spark Pac4j with Facebook OAuth

   @author pconrad
 */
public class SparkFacebookOAuthDemo {

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
	    System.out.println("No facebook profile");
	    return model;
	}
	try {
	    // Here is where you can try getting an OAuth Token and using it
	    // to access the Facebook API
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return model;
    }
    
    private static Map buildModel(Request request, Response response) {

	final Map model = new HashMap<String,Object>();
	
	Map<String, Object> map = new HashMap<String, Object>();
	for (String k: request.session().attributes()) {
	    Object v = request.session().attribute(k);
	    map.put(k,v);
	}
	
	model.put("session", map.entrySet());
	
	java.util.List<CommonProfile> userProfiles = getProfiles(request,response);

	map.put("profiles", userProfiles);

	try {
	    if (userProfiles.size()>0) {
		CommonProfile firstProfile = userProfiles.get(0);
		map.put("firstProfile", firstProfile);	
		
		FacebookProfile fp = (FacebookProfile) firstProfile;
		model.put("fp", fp); /*
		model.put("userid",fp.getUsername());
		model.put("name",fp.getDisplayName());
		model.put("avatar_url",fp.getPictureUrl());
		model.put("email",fp.getEmail()); */
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	*/
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
