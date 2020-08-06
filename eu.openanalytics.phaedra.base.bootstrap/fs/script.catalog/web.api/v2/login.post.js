var user = request.getParameter("user");
var pass = request.getParameter("password");
var token = security.authenticate(user, pass, request);
response.getWriter().write(token);