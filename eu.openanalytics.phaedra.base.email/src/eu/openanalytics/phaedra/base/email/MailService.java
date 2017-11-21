package eu.openanalytics.phaedra.base.email;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.email.model.DistributionList;
import eu.openanalytics.phaedra.base.environment.Screening;

public class MailService extends BaseJPAService {

	private String smtpHost;
	private String mailSuffix;
	
	private static MailService instance = new MailService();
	
	private MailService() {
		// Hidden constructor
		smtpHost = Screening.getEnvironment().getConfig().getValue("email.smtp.host");
		mailSuffix = Screening.getEnvironment().getConfig().getValue("email.suffix");
		if (mailSuffix == null || mailSuffix.isEmpty()) mailSuffix = "phaedra.org";
	}
	
	public static MailService getInstance() {
		return instance;
	}
	
	public String getMailSuffix() {
		return mailSuffix;
	}
	
	public List<DistributionList> getAllLists() {
		List<DistributionList> list = getList(DistributionList.class);
		return list;
	}
	
	public void createList(String name, String label) {
		DistributionList newList = new DistributionList();
		newList.setName(name);
		newList.setLabel(label);
		save(newList);
	}
	
	public void renameList(String name, String label, long id) {
		DistributionList newList = getList(id);
		newList.setName(name);
		newList.setLabel(label);
		save(newList);
	}
	
	public void deleteList(String listName) throws MailException {
		DistributionList list = getList(listName);
		if (list == null) throw new MailException("Distribution list does not exist: " + listName);
		delete(list);
	}
	
	public String[] getSubscribers(String listName) throws MailException {
		DistributionList list = getList(listName);
		if (list == null) throw new MailException("Distribution list does not exist: " + listName);
		String[] subscribers = new String[list.getSubscribers().size()];
		return list.getSubscribers().toArray(subscribers);
	}
	
	public void addSubscriber(String subscriber, String listName) throws MailException {
		DistributionList list = getList(listName);
		if (list == null) throw new MailException("Distribution list does not exist: " + listName);
		list.getSubscribers().add(subscriber);
		save(list);
	}
	
	public void removeSubscriber(String subscriber, String listName) throws MailException {
		DistributionList list = getList(listName);
		if (list == null) throw new MailException("Distribution list does not exist: " + listName);
		list.getSubscribers().remove(subscriber);
		save(list);
	}
	
	public void sendMail(String from, String toListName, String subject, String body, URL[] attachments) throws MailException {
		sendMail(from, toListName, subject, body, false, attachments);
	}
	
	public void sendMail(String from, String toListName, String subject, String body, boolean asHTML, URL[] attachments) throws MailException {
		String[] to = getSubscribers(toListName);
		sendMail(from, to, null, subject, body, asHTML, attachments);
	}
	
	public void sendMail(String from, String[] to, String[] cc, String subject, String body, boolean asHTML, URL[] attachments) throws MailException {

		if (to == null || to.length == 0) throw new MailException("Cannot send mail: no recipient specified");
		if (smtpHost == null) throw new MailException("Cannot send mail: no SMTP server configured");
		
		try {
			if (from == null) from = "phaedra@" + getMailSuffix();
			if (subject == null) subject = "";
			
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);
			Session session = Session.getDefaultInstance(props, null);

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			for (String recipient: to) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			}
			if (cc != null) {
				for (String recipient: cc) {
					message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipient));
				}
			}
			
			message.setSubject(subject);

			// Body: text
			if (body == null) body = "";
			Multipart multipart = new MimeMultipart("mixed");
			
			BodyPart textBodyPart = new MimeBodyPart();
			if (asHTML) textBodyPart.setContent(body, "text/html");
			else textBodyPart.setText(body);
			multipart.addBodyPart(textBodyPart);
			
			// Optional: attachments
			if (attachments != null && attachments.length > 0) {
				for (URL attachment: attachments) {
					BodyPart attachmentBodyPart = new MimeBodyPart();
					attachmentBodyPart.setDataHandler(new DataHandler(attachment));
					attachmentBodyPart.setFileName(attachment.getFile());
					multipart.addBodyPart(attachmentBodyPart);
				}
			}

			message.setContent(multipart);
			Transport.send(message);
		} catch (Exception e) {
			throw new MailException("Failed to send email", e);
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}
	
	private DistributionList getList(String name) {
		DistributionList list = getEntity("select l from DistributionList l where l.name = ?1", DistributionList.class, name);
		return list;
	}
	private DistributionList getList(long id) {
		DistributionList list = getEntity("select l from DistributionList l where l.id = ?1", DistributionList.class, id);
		return list;
	}
}
