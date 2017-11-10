/*
 * This script can be called from Phaedra clients using the following console command:
 * 
 * API.get("ScriptService").getCatalog().run("misc/sampleScript", [], true)
 * 
 * Argument 1: The name of the script to run, including any subfolder names. The .js extension may be omitted.
 * Argument 2: An array of parameters to pass into the script as an object named 'args'.
 * Argument 3: True to run the script asynchronously, false to run the script in the UI thread (not recommended).
 */

console.print("Hello world, I am sampleScript!");
console.print("I have received " + args.length + " arguments.");

var a = parseInt(100 * Math.random());
var b = parseInt(100 * Math.random());

console.print("Did you know that " + a + " * " + b + " = " + (a*b) + "?");