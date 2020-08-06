response.setContentType("text/html");
response.getWriter().write("<html><head><title>API Overview</title></head>");
response.getWriter().write("<body>");

response.getWriter().write("<h1>Login Form</h1>");

response.getWriter().write("<form method='POST' action='/api/login'>");

response.getWriter().write("Username: <input type='text' name='user'>");
response.getWriter().write("<br/><br/>");
response.getWriter().write("Password: <input type='password' name='password'>");
response.getWriter().write("<br/><br/>");
response.getWriter().write("<input type='submit' value='Log In'>");

response.getWriter().write("</form>");

response.getWriter().write("<p>");
response.getWriter().write("If the login succeeds, you will receive a token.<br/>");
response.getWriter().write("You can use this token in your API calls by appending ?token={your token} to the end of the URL.");
response.getWriter().write("</p>");

response.getWriter().write("</body>");
response.getWriter().write("</html>");