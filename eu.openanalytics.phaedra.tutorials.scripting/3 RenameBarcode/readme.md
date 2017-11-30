## RenameBarcode

This data capture script will modify the barcodes of readings being captured.
It appends an underscore followed by the first 4 characters of the parent folder name to the barcode.

E.g. if a reading being captured has the following source path:

	/some/path/to/2943experiment/reading001
	
Then the barcode for "reading001" will be changed into "reading001_2943".

### Running the renameBarcode script

Being a data capture script, this script cannot be executed directly via the [ScriptService](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/base/scripting/api/ScriptService.html).

Instead, it must be deployed by an administrator into the **data.capture.modules** directory on Phaedra's file server.
It can then be referenced in a data capture configuration XML file, using the following syntax:

	<module id="renameBarcodes" name="Rename Reading Barcodes" type="ScriptedModule">
		<parameters>
			<parameter key="script.id">renameBarcodes</parameter>
		</parameters>
	</module>

_Note:_ the parameter _script.id_ should refer to the script filename, without the extension.

To edit a protocol's data capture configuration, follow these steps:

1. [Edit the protocol class](https://www.phaedra.io/howtos/edit-protocol-class)
2. Click the _Edit_ button at the field *Default Capture Configuration**
3. Insert a module tag at the appropriate location
4. Save and close the XML file

To run a data capture job, see [How To: Import Plates](https://www.phaedra.io/howtos/import-plates)

### Script explained 

The script iterates over the array of **readings** that are in the process of being captured.
For each reading, it will:

* Retrieve the current barcode using _reading.getBarcode()_, and the path where the reading was found using _reading.getSourcePath()_.
* Compute a new barcode using these two values
* Set the reading's barcode to this new value.

_Note:_ there is no need to call any API to save the changes. The reading objects are automatically saved when the data capture job completes.

_Note:_ the JavaScript version contains a load statement:

	load("script://dc/common.js");
	
This will load the file "dc/common.js" from Phaedra's script catalog. This file contains a number of convenience functions for scripting,
including the _forEachReading_ function that is used here. Note that this script loading method is only available in JavaScript.