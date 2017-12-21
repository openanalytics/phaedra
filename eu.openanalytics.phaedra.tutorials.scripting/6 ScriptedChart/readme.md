## ScriptedChart

This script will generate a 2-dimensional scatter plot of two subwell features for one well.

This is a 'catalog script', which means it should be deployed into Phaedra's [script catalog](https://www.phaedra.io/howtos/scripting) by an administrator.
Either the JavaScript file or the Python file can be deployed in the catalog (it makes no sense to deploy both).

### Running the script

1. Make sure the script is deployed into Phaedra's script catalog.
2. Open the _Scripted Chart_ view by selecting _Window > Show View > Other... > Scripted Chart_.
3. In the view, select the script from the list of available scripts.
4. Click _Run_

### Script explained 

The script requires 4 parameters to run: _plateId_, _wellPos_, _f1Name_ and _f2Name_.
These parameters are set inside the script, so make sure to modify them appropriately before executing the script.

The script will then look up the plate and well, and retrieve the subwell data for that well for the 2 given subwell features.
This data, stored as 2 numeric arrays in _d1_ and _d2_, is plotted using the [matplotlib](https://matplotlib.org/) plotting library
for Python.

The plot is written as a PNG image into a memory buffer, which is then copied into the _output_ object.
This _output_ object represents a Java [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html), and will forward the image internally to be rendered onto the _Scripted Chart_ view.

Note: the _Scripted Chart_ view currently supports the following image formats:  BMP, ICO, JPEG, GIF, PNG, TIFF.

Note: to install the _matplotlib_ library, follow these steps:

* Locate the Python folder:
* On Windows: `<phaedra installation folder>\plugins\org.python_2.7.3\os\win32\x86_64\python`
* On Linux: `<phaedra installation folder>\plugins\org.python_2.7.3\os\linux\x86_64\python`
* On Mac: The Mac client does not contain a Python installation. Instead, the default Python installation of your system is used.
* Open a console and navigate to this folder
* Enter the following commands:
* `python -mpip install -U pip`
* `python -mpip install -U matplotlib`