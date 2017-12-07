## FeatureSummary

This script will iterate over all well features for a given plate and calculate the min and max values for each numeric feature.
The summary will be printed in the console view.

This is a 'catalog script', which means it should be deployed into Phaedra's [script catalog](https://www.phaedra.io/howtos/scripting) by an administrator.
Either the JavaScript file or the Python file can be deployed in the catalog (it makes no sense to deploy both).

### Running the featureSummary script

1. Make sure the script is deployed into Phaedra's script catalog.
2. Navigate to a plate to use the script on.
3. Right-click on the plate and select _Scripts > Run Script from Catalog_.
4. Select the _featureSummary_ script from the list of available scripts. 
5. Click OK

### Script explained 

The script accepts 1 input argument: **plateId**, which is used to look up a plate.
Then, the [CalculationService](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/calculation/CalculationService.html) API is used to obtain a [PlateDataAccessor](https://www.phaedra.io/javadoc/eu/openanalytics/phaedra/calculation/PlateDataAccessor.html), which in turn provides access to the
plate's well data.

A loop is performed over each feature, skipping non-numeric features. For each feature, the min and max values are computed and stored in a map/dict structure named **summary**.

Note that the raw well values are used, as opposed to normalized values.
This can be seen in the call

	value = dataAccessor.getNumericValue(nr, feature, None)

Where the first argument is the well number, the second is the feature and the third is the normalization method.

At the end of the script, this summary is printed out into the scripting enginge's console.
