## CustomFileParser

This welldata parser script will parse data from a text file with the following format:

	Feature1Name
	
	WellA1Value	WellA2Value	...
	WellB1Value	WellB2Value	...
	...
	
	Feature2Name
	
	WellA1Value	WellA2Value	...
	WellB1Value	WellB2Value	...
	...

This format, where each feature is represented by a rectangular block of values, is not compatible with the
CSV (comma-separated values) format expected by the default _txt.welldata.parser.js_ parser,
and thus requires the addition of a custom parser.

### Running the script

Being a welldata parser script, this script cannot be executed directly via the [ScriptService](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/base/scripting/api/ScriptService.html).

Instead, it must be deployed by an administrator into the **data.parsers** directory on Phaedra's file server.
It can then be referenced in a data capture configuration XML file, using the following syntax:

	<module id="gather.welldata" name="Gather well data" type="ScriptedModule">
		<parameters>
			<parameter key="script.id">capture.welldata</parameter>
			<parameter key="parser.id">custom.file.parser</parameter>
		</parameters>
	</module>

_Note:_ the parameter _script.id_ should refer to the "capture.welldata" module, which will invoke the parser.
The parameter _parser.id_ should refer to the parser script filename, without the extension.

To edit a protocol's data capture configuration, follow these steps:

1. [Edit the protocol class](https://www.phaedra.io/howtos/edit-protocol-class)
2. Click the _Edit_ button at the field *Default Capture Configuration**
3. Insert a module tag at the appropriate location
4. Save and close the XML file

To run a data capture job, see [How To: Import Plates](https://www.phaedra.io/howtos/import-plates)

### Script explained 

All data parser scripts automatically receive an object named **data**. This is a byte array, representing the contents of the
file (or other source) being parsed.

_Note:_ there is also a **dataStream** object available, which represents an InputStream of the data source. In this tutorial, the _data_
object is used instead.

* First, the data is split into lines using the [ModelUtils](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/datacapture/parser/util/ModelUtils.html) utility class.
* Next, the lines are iterated, and the data of each line is appended to a data "block", representing the rectangular block
of values for the current feature.
* Each time a new feature name is encountered, the current data block is saved into the parser's
[model](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/datacapture/parser/model/ParsedModel.html) object.

This _model_ object is the key object of the parser: when the data capture job is complete, all data in the model will be saved into Phaedra.
You can insert any number of well and/or subwell features in a model object, but you must also keep in mind that this data is being kept in memory
during the execution of the parser. If one huge file contains many gigabytes worth of data, you should consider splitting the file up, or
executing multiple parsers that each parse a portion of the file.