package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.columbus.ColumbusService;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements.Measurement;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates.Plate;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;
import eu.openanalytics.phaedra.ui.columbus.Activator;
import eu.openanalytics.phaedra.ui.columbus.importwizard.Meas.MeasAnalysis;

public class ColumbusImportHelper {

	public static Meas[] findMeasSources(Screen screen, String instanceId) {
		List<Meas> measurements = new ArrayList<>();
		try (ColumbusWSClient client = ColumbusService.getInstance().connect(instanceId)) {
			List<Plate> plates = client.executeList(new GetPlates(screen.screenId));
			for (Plate plate: plates) {
				List<Measurement> plateMeasurements = client.executeList(new GetMeasurements(plate.plateId, screen.screenId));
				for (Measurement meas: plateMeasurements) {
					measurements.add(getMeas(plate, meas, client));
				}
			}
		} catch (IOException e) {
			EclipseLog.error("Failed to query Columbus measurements", e, Activator.getDefault());
		}
		return measurements.toArray(new Meas[measurements.size()]);
	}
	
	public static User[] getColumbusUsers(String instanceId) {
		List<User> users = new ArrayList<>();
		try (ColumbusWSClient client = ColumbusService.getInstance().connect(instanceId)) {
			users = ColumbusService.getInstance().getUsers(client);
		} catch (IOException e) {
			EclipseLog.error("Failed to query Columbus users", e, Activator.getDefault());
		}
		Collections.sort(users, (u1, u2) -> u1.loginname.compareTo(u2.loginname));
		return users.toArray(new User[users.size()]);
	}
	
	public static Screen[] getColumbusScreens(User user, String instanceId) {
		List<Screen> screens = new ArrayList<>();
		try (ColumbusWSClient client = ColumbusService.getInstance().connect(instanceId)) {
			screens = client.executeList(new GetScreens(user.userId));
		} catch (IOException e) {
			EclipseLog.error("Failed to query Columbus screens", e, Activator.getDefault());
		}
		return screens.toArray(new Screen[screens.size()]);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private static Meas getMeas(Plate plate, Measurement meas, ColumbusWSClient client) throws IOException {
		Meas m = new Meas();
		m.name = meas.measurementDate.toString();
		m.barcode = plate.plateName;
		m.source = "" + meas.measurementId;
		m.isIncluded = true;
		
		m.availableAnalyses = client.executeList(new GetResults(meas.measurementId)).stream()
			.sorted((r1, r2) -> r2.resultDate.compareTo(r1.resultDate))
			.map(r -> {
				MeasAnalysis a = new MeasAnalysis();
				a.name = r.resultName;
				a.source = "" + r.resultId;
				return a;
			})
			.collect(Collectors.toList())
			.toArray(new MeasAnalysis[0]);
		
		if (m.availableAnalyses.length > 0) m.selectedAnalysis = m.availableAnalyses[0];
		else m.isIncluded = false;
		return m;
	}
}
