var memTotal1 = java.lang.Runtime.getRuntime().totalMemory(); 
var memFree1 = java.lang.Runtime.getRuntime().freeMemory();

java.lang.System.gc();

var memTotal2 = java.lang.Runtime.getRuntime().totalMemory(); 
var memFree2 = java.lang.Runtime.getRuntime().freeMemory();
var memMax = java.lang.Runtime.getRuntime().maxMemory();

response.setContentType("application/json");
  response.getWriter().write(JSON.stringify({
    before: { free: memFree1, total: memTotal1 },
    after: { free: memFree2, total: memTotal2 },
    max: memMax
}, null, 2));