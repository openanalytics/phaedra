## HelloWorld

This simple script will display the message "Hello World!" in the console.

There are two versions:

* helloWorld.js: this is a JavaScript file
* helloWorld.py: this is a Python file

### Running helloWorld.js

1. Open the Console View: select _Window > Show View > Other... > General > Console._
2. Switch to the JavaScript console: select the arrow next to the blue console icon in the console's toolbar, and select _JavaScript_.
3. Type the following command:

	API.get("ScriptService").executeScriptFile("/.../path/to/helloWorld.js", {})
	
### Running helloWorld.py

1. Make sure you have the [Python addon](https://www.phaedra.io/addons/python) installed.
2. Open the Console View: select _Window > Show View > Other... > General > Console._
3. Switch to the Python console: select the arrow next to the blue console icon in the console's toolbar, and select _Python_.
3. Type the following command:

	API.get("ScriptService").executeScriptFile("/.../path/to/helloWorld.py", None)

### Script explained 

The script contains just a single statement, that prints a String message to the console.

Any user can execute this script file, because using the [ScriptService API](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/base/scripting/api/ScriptService.html) does not require any special privileges:

	API.get("ScriptService").executeScriptFile(pathToFile, parameterMap)

The script itself also requires no special privileges: any user can print to the console.
Should the script modify a plate's barcode, however, that would be a different story!
