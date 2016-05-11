/*
 * This script is called by the Columbus Protocol Wizard, to create a protocol template.
 * The following objects are passed into this script:
 *
 * - template: A StringBuilder where the template should be written to
 * - protocolName: The name for the protocol
 * - protocolTeam: The team for the protocol
 * - screen: The Columbus Screen that was used to configure the channels and features
 * - imageChannels: An array of image channels
 * - imageChannelPatterns: An array of file patterns of each image channel
 * - wellFeatures: An array of well features
 * - subwellFeatures: An array of subwell features
 * - parameters: A map containing arbitrary data passed in from the screen analyzer script
 */

template.append("protocol.name=" + protocolName + "\n");
template.append("protocol.team=" + protocolTeam + "\n");
template.append("protocolclass.name=" + protocolName + "\n");

template.append("template=columbus\n");

var swPattern = parameters.get("subwelldata.filepattern");
if (swPattern != null) template.append("subwelldata.filepattern=" + swPattern + "\n");

var extPath = parameters.get("externalPath");
if (extPath != null) {
	template.append("subwelldata.path=" + extPath + "\\${experimentName}\\${barcode}\n");
	template.append("imagedata.path=" + extPath + "\\${experimentName}\\${barcode}\n");
}

for (var i in imageChannels) {
	template.append("imagedata.channel." + (i+1) + ".name=" + imageChannels[i].getName() + "\n");
	template.append("imagedata.channel." + (i+1) + ".filepattern=" + imageChannelPatterns[i] + "\n");
	template.append("imagedata.channel." + (i+1) + ".color=" + imageChannels[i].getColorMask() + "\n");
	
	if (imageChannels[i].getType() == 0) {
		template.append("imagedata.channel." + (i+1) + ".contrast.min=" + imageChannels[i].getLevelMin() + "\n");
		template.append("imagedata.channel." + (i+1) + ".contrast.max=" + imageChannels[i].getLevelMax() + "\n");
		template.append("imagedata.channel." + (i+1) + ".depth=" + imageChannels[i].getBitDepth() + "\n");
	} else {
		template.append("imagedata.channel." + (i+1) + ".type=Overlay\n");
	}
	
	if (imageChannels[i].getChannelConfig().get("source") == "columbus") {
		template.append("imagedata.channel." + (i+1) + ".source=columbus\n");
	}
	if (imageChannels[i].getChannelConfig().get("montage") == "true") {
		template.append("imagedata.channel." + (i+1) + ".montage=true\n");
	}
}

var featureNr = 1;
for (var i in wellFeatures) {
	var f = wellFeatures[i];
	if (!f.addFeatureToProtocolClass) continue;
	template.append("wellfeature." + featureNr + ".name=" + f.name + "\n");
	template.append("wellfeature." + featureNr + ".key=" + f.isKey + "\n");
	template.append("wellfeature." + featureNr + ".numeric=" + f.isNumeric + "\n");
	featureNr++;
}

featureNr = 1;
for (var i in subwellFeatures) {
	var f = subwellFeatures[i];
	if (!f.addFeatureToProtocolClass) continue;
	template.append("subwellfeature." + featureNr + ".name=" + f.name + "\n");
	template.append("subwellfeature." + featureNr + ".key=" + f.isKey + "\n");
	template.append("subwellfeature." + featureNr + ".numeric=" + f.isNumeric + "\n");
	featureNr++;
}