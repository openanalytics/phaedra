sb = new java.lang.StringBuilder();
tBean = java.lang.management.ManagementFactory.getThreadMXBean();
infos = tBean.getThreadInfo(tBean.getAllThreadIds(), 100);
for (var i in infos) {
	sb.append("'");
    sb.append(infos[i].getThreadName());
    sb.append("'");
    var stack = infos[i].getStackTrace();
    for (var j in stack) {
    	sb.append("\n\t at ");
        sb.append(stack[j]);
    }
    sb.append("\n\n");
}
response.getWriter().write(sb.toString());