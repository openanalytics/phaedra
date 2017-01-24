package eu.openanalytics.phaedra.datacapture.columbus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.ImageData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetFields;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetFields.Field;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetImage;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements.Measurement;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates.Plate;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResult;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults.Result;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetWells;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetWells.Well;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.AbstractModule;

//TODO Support for downloading measurements without analysis results?
public class ColumbusDownloadModule extends AbstractModule {

	public final static String TYPE = "ColumbusDownloadModule";
	
	private final static String PARAM_DL_IMAGE_DATA = "download.image.data";
	private final static String PARAM_DL_MEAS_DATA = "download.meas.data";
	private final static String PARAM_DL_RES_DATA = "download.res.data";
	
	private String tempFolder;
	
	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		DataCaptureTask task = context.getTask();
		String[] columbusPath = task.getSource().split("/");
		if (columbusPath.length < 3) throw new DataCaptureException("Invalid source path:" + task.getSource() + ". Expected: Columbus/<userName>/<screenName>");
		String userName = columbusPath[1];
		String screenName = task.getSource().substring((columbusPath[0] + "/" + columbusPath[1] + "/").length());
		
		// If user provided a custom experiment name, make sure ${experimentName} still refers to the original screen name.
		String targetExpName = (String) task.getParameters().get(DataCaptureTask.PARAM_EXPERIMENT_NAME);
		if (targetExpName != null) task.getParameters().put(DataCaptureTask.PARAM_TARGET_EXPERIMENT_NAME, targetExpName);
		task.getParameters().put(DataCaptureTask.PARAM_EXPERIMENT_NAME, screenName);
		
		monitor.beginTask("Downloading data from Columbus", 100);
		context.getLogger().info("Downloading data from Columbus screen " + screenName);
		
		ExecutorService imageDownloader = isEnabled(PARAM_DL_IMAGE_DATA) ? Executors.newWorkStealingPool(PrefUtils.getNumberOfThreads()) : null;
		ExecutorService imageSplitter = isEnabled(PARAM_DL_IMAGE_DATA) ? Executors.newWorkStealingPool(PrefUtils.getNumberOfThreads()) : null;
		AtomicInteger imageWorkCount = new AtomicInteger(0);
		
		Map<Long,Long> resultIds = ColumbusService.getInstance().getResultIds(task.getParameters());
		try (ColumbusWSClient client = ColumbusService.getInstance().connect(task.getParameters())) {
			Screen screen = ColumbusService.getInstance().getScreens(client, userName).stream().filter(s -> screenName.equals(s.screenName)).findFirst().orElse(null);
			if (screen == null) throw new DataCaptureException("Screen not found: " + task.getSource());
			long screenId = screen.screenId;
			
			tempFolder = FileUtils.generateTempFolder(true);
			String expDir = tempFolder + "/" + screenId;
			int readingSequence = 1;
			
			GetPlates getPlates = new GetPlates(screenId);
			client.execute(getPlates);
			for (Plate plate: getPlates.getList()) {
				GetMeasurements getMeasurements = new GetMeasurements(plate.plateId, screenId);
				client.execute(getMeasurements);
				
				// Retrieve measurements, sorted by date.
				List<Measurement> measurements = getMeasurements.getList();
				Collections.sort(measurements, (m1, m2) -> m1.measurementDate.compareTo(m2.measurementDate));
				
				for (int i=0; i<measurements.size(); i++) {
					if (monitor.isCanceled()) break;
					
					Measurement meas = measurements.get(i);
					String measDir = expDir + "/" + plate.plateName + "--Me" + meas.measurementId;

					// Use either the specified resultId, or the latest available.
					Result result = null;
					if (resultIds != null) {
						Long resultId = resultIds.get(meas.measurementId);
						if (resultId != null) {
							List<Result> results = client.executeList(new GetResults(meas.measurementId));
							result = results.stream().filter(r -> r.resultId == resultId).findAny().orElse(null);
						}
					} else {
						result = ColumbusService.getInstance().getLatestResult(client, meas.measurementId);
					}
					if (result == null) continue;
					
					if (isEnabled(PARAM_DL_RES_DATA)) {
						// Write the result data to a file.
						GetResult getResult = new GetResult(result.resultId);
						client.execute(getResult);
						String resultValue = getResult.getResultValue();
						new File(measDir).mkdirs();
						FileUtils.write(resultValue.getBytes("UTF-8"), measDir + "/Results_Analysis.xml", false);
					}
					
					String uniqueIdentifier = ColumbusService.getInstance().getUniqueResultId(client, result.resultId);
					PlateReading reading = context.createNewReading(readingSequence, uniqueIdentifier);
					reading.setSourcePath(measDir);
					reading.setBarcode(plate.plateName + "--Me" + meas.measurementId);
					readingSequence++;
					
					// Save some meas and result information as plate properties.
					context.getStore(reading).setProperty("/", "MeasId", meas.measurementId);
					context.getStore(reading).setProperty("/", "ResultId", result.resultId);
					if (meas.measurementDate != null) context.getStore(reading).setProperty("/", "MeasDate", meas.measurementDate.toString());
					if (result.resultName != null) context.getStore(reading).setProperty("/", "ResultName", result.resultName);
					if (result.resultDate != null) context.getStore(reading).setProperty("/", "ResultDate", result.resultDate.toString());
					
					// Keep a set of fields from a random well to build the Meas file later.
					List<Field> fields = new ArrayList<>();
					
					// Parallel download of images
					GetWells getWells = new GetWells(meas.measurementId);
					client.execute(getWells);
					int wellCount = getWells.getList().size();
					for (Well well: getWells.getList()) {
						try {
							GetFields getFields = new GetFields(well.wellId, meas.measurementId);
							client.execute(getFields);
							if (fields.isEmpty()) fields.addAll(getFields.getList());
							if (isEnabled(PARAM_DL_IMAGE_DATA)) {
								for (Field field: getFields.getList()) {
									String destination = measDir + "/" + well.wellName + "_Field" + field.field + ".tif";
									imageWorkCount.incrementAndGet();
									imageDownloader.submit(new ImageDownloader(client, imageSplitter, imageWorkCount, field, destination));
								}
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					
					if (isEnabled(PARAM_DL_MEAS_DATA)) {
						// Store layout information in a separate MEAS file
						String destination = measDir + "/Meas_" + meas.measurementId + ".xml";
						Document doc = createMeasDoc(plate, fields, wellCount);
						String docString = XmlUtils.writeToString(doc);
						FileUtils.write(docString.getBytes("UTF-8"), destination, false);
					}
				}
			}
			monitor.worked(20);

			if (isEnabled(PARAM_DL_IMAGE_DATA)) {
				while (imageWorkCount.get() > 0) {
					if (monitor.isCanceled()) break;
					monitor.subTask("Downloading images: " + imageWorkCount.get() + " remaining.");
					try { Thread.sleep(500); } catch (InterruptedException e) {}
				}
				imageDownloader.shutdownNow();
				imageSplitter.shutdownNow();
			}
		} catch (Exception e) {
			throw new DataCaptureException("Failed to download Columbus screen " + screenName, e);
		}
		
		monitor.done();
	}
	
	@Override
	public void postCapture(DataCaptureContext context, IProgressMonitor monitor) {
		if (tempFolder != null) FileUtils.deleteRecursive(new File(tempFolder));
	}
	
	@Override
	public int getWeight() {
		if (isEnabled(PARAM_DL_IMAGE_DATA)) return 40;
		else return 1;
	}
	
	private Document createMeasDoc(Plate plate, List<Field> fields, int wellCount) throws IOException {
		// Store layout information in a separate MEAS file
		Document doc = XmlUtils.createEmptyDoc(); 
		Element meas = XmlUtils.createTag(doc, doc, "Measurement");
		
		Element el = XmlUtils.createTag(doc, meas, "Barcode");
		el.setTextContent(plate.plateName);
		
		el = XmlUtils.createTag(doc, meas, "PlateLayout");
		el = XmlUtils.createTag(doc, el, "PlateDescription");
		
		//Note: this layout determination only works for 96 or 384 well plates.
		int rows = 8;
		if ((plate.plateType != null && plate.plateType.contains("384")) || wellCount > 96) rows = 16;
		el.setAttribute("Columns", String.valueOf((int)(1.5*rows)));
		el.setAttribute("Rows", String.valueOf(rows));
		
		el = XmlUtils.createTag(doc, meas, "Areas");
		el = XmlUtils.createTag(doc, el, "Area");
		el = XmlUtils.createTag(doc, el, "Sublayout");
		
		Collections.sort(fields, (f1,f2) -> f1.field - f2.field);
		for (Field field: fields) {
			Element point = XmlUtils.createTag(doc, el, "Point");
			point.setAttribute("x", String.valueOf(field.posX));
			point.setAttribute("y", String.valueOf(field.posY));
		}
		
		return doc;
	}
	
	private boolean isEnabled(String param) {
		Object paramValue = getConfig().getParameters().getParameter(param);
		if (paramValue == null) return true; // Enabled by default.
		return (paramValue instanceof String && Boolean.valueOf((String)paramValue));
	}
	
	private static class ImageDownloader implements Callable<String> {
		
		private ColumbusWSClient client;
		private ExecutorService exec;
		private AtomicInteger imageWorkCount;
		private Field field;
		private String destination;
		
		public ImageDownloader(ColumbusWSClient client, ExecutorService exec, AtomicInteger imageWorkCount, Field field, String destination) {
			this.client = client;
			this.exec = exec;
			this.imageWorkCount = imageWorkCount;
			this.field = field;
			this.destination = destination;
		}
		
		@Override
		public String call() throws Exception {
			try (OutputStream out = new FileOutputStream(destination)) {
				client.execute(new GetImage(field.imageId, out));
				exec.submit(new ImageSplitter(destination, imageWorkCount));
				return null;
			}
		}
	}
	
	private static class ImageSplitter implements Callable<String> {
		
		private String source;
		private AtomicInteger imageWorkCount;
		
		public ImageSplitter(String source, AtomicInteger imageWorkCount) {
			this.source = source;
			this.imageWorkCount = imageWorkCount;
		}
		
		@Override
		public String call() throws Exception {
			ImageData[] channels = TIFFCodec.read(source);
			new File(source).delete();
			for (int ch=0; ch<channels.length; ch++) {
				String splitDestination = source.replace(".tif", "_Ch" + (ch+1) + ".tif");
				TIFFCodec.write(channels[ch], splitDestination);
			}
			imageWorkCount.decrementAndGet();
			return null;
		}
	}
}
