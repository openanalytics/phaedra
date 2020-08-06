var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
var well = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Well").class, new java.lang.Long(id));
image = API.get("ImageRenderService").getImageData(generateImageRequest(well));
