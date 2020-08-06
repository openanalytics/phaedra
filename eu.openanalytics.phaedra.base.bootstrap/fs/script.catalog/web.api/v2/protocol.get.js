load("script://web.api/header.js");

var id = parseInt(urlParts[0]);
var protocol = API.get("ProtocolService").getProtocol(id);

var map = {
	"id": protocol.getId(),
	"name": protocol.getName(),
	"description": protocol.getDescription(),
	"protocolClassId": protocol.getProtocolClass().getId(),
	"team": protocol.getTeamCode(),
	"uploadSystem": protocol.getUploadSystem()
};

response.setContentType("application/json");
response.getWriter().write(JSON.stringify(map));