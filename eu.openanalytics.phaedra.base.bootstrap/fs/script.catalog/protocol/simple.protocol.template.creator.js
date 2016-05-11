template.append("protocol.name=" + parameters.get("protocol.name") + "\n");
template.append("protocol.team=" + parameters.get("protocol.team") + "\n");
template.append("protocolclass.name=" + parameters.get("protocol.name") + "\n");

template.append("template=simple\n");

template.append("plate.folderpattern=(.+)\n");

template.append("welldata.path=" + parameters.get("welldata.path") + "\n");
template.append("welldata.filepattern=" + parameters.get("welldata.filepattern") + "\n");

if (parameters.get("subwelldata.path") != null) {
	template.append("subwelldata.path=" + parameters.get("subwelldata.path") + "\n");
	template.append("subwelldata.filepattern=" + parameters.get("subwelldata.filepattern") + "\n");
}

if (parameters.get("imagedata.path") != null) {
	template.append("imagedata.path=" + parameters.get("imagedata.path") + "\n");
	
	var i = 1;
	while (parameters.get("imagedata.channel." + i + ".name") != null) {
		template.append("imagedata.channel." + i + ".name=" + parameters.get("imagedata.channel." + i + ".name") + "\n");
		template.append("imagedata.channel." + i + ".filepattern=" + parameters.get("imagedata.channel." + i + ".filepattern") + "\n");
		i++;
	}
}