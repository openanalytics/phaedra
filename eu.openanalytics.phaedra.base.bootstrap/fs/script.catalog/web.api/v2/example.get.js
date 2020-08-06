/*
 * Example of a script implementing a RESTful HTTP API.
 * 
 * The URL scheme is:
 * http://{hostname}/api/{version}/{component}/{arg0}/{arg1}/...
 * 
 * The URL is mapped to a script as follows:
 * 1. The {component} is the base script name
 * 2. The request method (GET, PUT, DELETE, ...) is appended to the script name in lower case.
 * 3. The script must reside on the file server under /script.catalog/web.api/.
 *
 * This example script will service GET requests on the following URL:
 * http(s)://{hostname}/api/v2/example
 * 
 * The following objects are available to the script:
 * 
 * request: This is the servlet's HttpServletRequest
 * response: This is the servlet's HttpServletResponse
 * urlParts: A String array containing the parts of the URL after the component (may be empty)
 * json: A utility class to convert objects into JSON format
 * security: A utility class to perform security-related tasks
 */

// This call will perform an authentication check, and include some common objects and functions.
load("script://web.api/header.js");

var output = new java.util.HashMap();
output.put("status", "Ok");
output.put("message", "You successfully called the Example script with " + urlParts.length + " additional arguments");

var json = json.toJson(output, null);
response.setContentType("application/json");
response.getWriter().write(json);