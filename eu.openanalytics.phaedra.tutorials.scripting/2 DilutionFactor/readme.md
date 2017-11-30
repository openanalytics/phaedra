## DilutionFactor

This script will modify the well concentrations in a given plate by dividing their current concentrations by a dilution factor.

There are two versions:

* dilutionFactor.js: this is a JavaScript file
* dilutionFactor.py: this is a Python file

This is a 'catalog script', which means it should be deployed into Phaedra's [script catalog](https://www.phaedra.io/howtos/scripting) by an administrator.
Either the JavaScript file or the Python file can be deployed in the catalog (it makes no sense to deploy both).

### Running the dilutionFactor script

1. Make sure the script is deployed into Phaedra's script catalog.
2. Navigate to a plate to use the script on.
3. Right-click on the plate and select _Scripts > Run Script from Catalog_.
4. Select the _dilutionFactor_ script from the list of available scripts. 
5. In the Arguments field, enter a dilutionFactor, e.g. 10
6. Click OK

### Script explained 

The script uses 2 input arguments: **plateId**  and **dilutionFactor** . As you can see, these arguments are obtained from an object named **args**.
This object is an array of arguments to the script.

* The first argument is always the ID of the plate the script is being run on.
* The second argument is the value the user typed in the Arguments field of the Run Script dialog.
* Any further arguments can also be typed in the Arguments field, using commas to separate multiple arguments.

The script uses 2 API services to perform its purpose:

* [PlateService](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/model/plate/PlateService.html): to retrieve the plate and its wells, and later on to save the changes to the well concentrations.
* [CalculationService](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/calculation/CalculationService.html): to perform a recalculation of the plate, which triggers the re-fitting of any dose-response curves.

A note on the Python script: due to a technical limitation of the Python-Java bridge, the **args** object is not an array, but rather a List.
Items can be retrieved using the _get(index)_ method, but care should be taken with the type of the value. It is often a Java String, Integer or Float,
and the safest approach is to first cast them into a Python String using _str()_, followed by a cast to the desired type.
