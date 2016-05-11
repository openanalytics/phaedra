package eu.openanalytics.phaedra.link.data.notification;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.email.MailException;
import eu.openanalytics.phaedra.base.email.MailService;
import eu.openanalytics.phaedra.base.hook.IBatchedHook;
import eu.openanalytics.phaedra.base.hook.IBatchedHookArguments;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem.LogItemSeverity;
import eu.openanalytics.phaedra.datacapture.log.IDataCaptureLogListener;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookArguments;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

/**
 * Notification sender for data capture and/or import jobs.
 *
 * The following triggers are supported:
 * <ul>
 * <li><b>Capture error</b>: send an error notification to the protocol subscribers and phaedra support.</li>
 * <li><b>Capture complete</b>: send a notification to phaedra support. Only if auto-link is disabled.</li>
 * <li><b>Data link complete</b>: send a notification to the protocol subscribers and phaedra support.</li>
 * </ul>
 */
public class EmailNotifier implements IDataCaptureLogListener, IBatchedHook {

	private final static String PHAEDRA_SUPPORT_LIST = "phaedra.support";
	private final static String PROTOCOL_LIST_PREFIX = "subscribers.protocol.";
	
	private List<LinkDataHookArguments> batchedArgs;
	
	public EmailNotifier() {
		DataCaptureService.getInstance().addLogListener(this);
	}

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		LinkDataHookArguments linkArgs = (LinkDataHookArguments)args;
		if (batchedArgs != null) {
			// Batch mode: add this outcome to the list.
			batchedArgs.add(linkArgs);
		} else {
			// Single mode: handle each outcome separately.
			batchedArgs = new ArrayList<>();
			batchedArgs.add(linkArgs);
			notifyLinkComplete();
			batchedArgs = null;
		}
	}

	@Override
	public void startBatch(IBatchedHookArguments args) {
		batchedArgs = new ArrayList<>();
	}

	@Override
	public void endBatch(boolean successful) {
		if (successful) {
			notifyLinkComplete();
		}
		batchedArgs = null;
	}
	
	@Override
	public void logEvent(DataCaptureLogItem item) {
		if (item.severity == LogItemSeverity.Completed && item.reading == null) notifyCaptureComplete(item);
		if (item.severity == LogItemSeverity.Error) notifyError(item);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private boolean isEnabled() {
		return DataCaptureService.getInstance().isServerEnabled();	
	}
	
	private void notifyLinkComplete() {
		if (!isEnabled()) return;
		if (batchedArgs.isEmpty()) return;
		
		String title = "Phaedra: import complete";
		String body = loadTemplate("import_complete.html");
		if (body == null) return;
		
		StringBuilder readingList = new StringBuilder();
		readingList.append("<ul>");
		Collections.sort(batchedArgs, (b1, b2) -> b1.plate.getSequence() - b2.plate.getSequence());
		for (LinkDataHookArguments args: batchedArgs) {
			String href = "<a href=\"phaedra://plate/" + args.plate.getId() + "\">" + args.reading.getBarcode() + "</a>";
			readingList.append("<li>" + args.plate.getSequence() + " - " + href + "</li>");
		}
		readingList.append("</ul>");
		body = body.replace("${readingList}", readingList.toString());
		body = body.replace("${readingCount}", ""+batchedArgs.size());
		body = body.replace("${protocolName}", batchedArgs.get(0).reading.getProtocol());
		body = body.replace("${experimentName}", batchedArgs.get(0).reading.getExperiment());
		
		Protocol p = batchedArgs.get(0).task.targetExperiment.getProtocol();
		String mailingList = PROTOCOL_LIST_PREFIX + p.getId();
		
		try {
			sendMail(title, body.toString(), mailingList);
		} catch (MailException e) {
			EclipseLog.error("Failed to send data capture notification", e, Activator.getDefault());
		}
	}
	
	private void notifyCaptureComplete(DataCaptureLogItem item) {
		if (!isEnabled()) return;
		
		Object allowAutoLink = item.task.getParameters().get(DataCaptureTask.PARAM_ALLOW_AUTO_LINK);
		if (allowAutoLink instanceof Boolean && (Boolean)allowAutoLink) {
			// Auto link is enabled: do nothing now. Let the post-link notifier do its work instead.
			return;
		}
		String protocol = (String)item.task.getParameters().get(DataCaptureTask.PARAM_PROTOCOL_NAME);
		
		String title = "Phaedra: capture complete";
		String body = loadTemplate("capture_complete.html");
		if (body == null) return;
		
		body = body.replace("${protocolName}", protocol);
		body = body.replace("${taskSource}", item.task.getSource());
		
		try {
			sendMail(title, body.toString(), PHAEDRA_SUPPORT_LIST);
		} catch (MailException e) {
			EclipseLog.error("Failed to send data capture notification", e, Activator.getDefault());
		}
	}
	
	private void notifyError(DataCaptureLogItem item) {
		if (!isEnabled()) return;
		
		String title = "Phaedra: capture error";
		String body = loadTemplate("capture_error.html");
		
		String protocol = (String)item.task.getParameters().get(DataCaptureTask.PARAM_PROTOCOL_NAME);
		
		body = body.replace("${protocolName}", protocol);
		body = body.replace("${taskSource}", item.task.getSource());
		body = body.replace("${errorMessage}", item.message);
		body = body.replace("${errorCause}", StringUtils.getStackTrace(item.errorCause));
		
		String mailingList = PHAEDRA_SUPPORT_LIST;
		List<Protocol> matches = ProtocolService.getInstance().getProtocolsByName(protocol);
		if (matches.size() == 1) mailingList = PROTOCOL_LIST_PREFIX + matches.get(0).getId();
		
		try {
			sendMail(title, body.toString(), mailingList);
		} catch (MailException e) {
			EclipseLog.error("Failed to send data capture notification", e, Activator.getDefault());
		}
	}
	
	private void sendMail(String title, String body, String mailingList) throws MailException {
		String sender = "phaedra-dc-server@" + MailService.getInstance().getMailSuffix();
		
		if (!PHAEDRA_SUPPORT_LIST.equalsIgnoreCase(mailingList)) {
			try {
				// Send a separate copy to phaedra support.
				MailService.getInstance().sendMail(sender, PHAEDRA_SUPPORT_LIST, title, body, true, null);
			} catch (MailException e) {}
		}
		
		// If the mailing list does not exist or is empty, abort the mail send.
		try {
			String[] to = MailService.getInstance().getSubscribers(mailingList);
			if (to == null || to.length == 0) {
				EclipseLog.warn("Cannot send notification: invalid mailing list: " + mailingList, Activator.getDefault());
				return;
			}
		} catch (MailException e) {
			EclipseLog.warn("Cannot send notification: invalid mailing list: " + mailingList, Activator.getDefault());
			return;
		}
		
		MailService.getInstance().sendMail(sender, mailingList, title, body, true, null);
	}

	private String loadTemplate(String name) {
		try {
			URL templateURL = Activator.getDefault().getBundle().getEntry("templates/" + name);
			return new String(StreamUtils.readAll(templateURL.openStream()));
		} catch (IOException e) {
			EclipseLog.error("Failed to load mail template '" + name + "'", e, Activator.getDefault());
			return null;
		}
	}
}
