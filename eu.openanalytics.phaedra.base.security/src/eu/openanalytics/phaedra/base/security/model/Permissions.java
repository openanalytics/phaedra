package eu.openanalytics.phaedra.base.security.model;

/**
 * Permissions are mappings between operations (such as creating or
 * editing an object of a specific type) and the role required
 * to perform that operation.
 */
public class Permissions {

	public static final String PROTOCOLCLASS_OPEN = Roles.READ_ONLY_USER;
	public static final String PROTOCOLCLASS_CREATE = Roles.USER;
	public static final String PROTOCOLCLASS_EDIT = Roles.DATA_MANAGER;
	public static final String PROTOCOLCLASS_DELETE = Roles.DATA_MANAGER;

	public static final String PROTOCOL_OPEN = Roles.READ_ONLY_USER;
	public static final String PROTOCOL_CREATE = Roles.USER;
	public static final String PROTOCOL_EDIT = Roles.USER;
	public static final String PROTOCOL_DELETE = Roles.DATA_MANAGER;

	public static final String EXPERIMENT_OPEN = Roles.READ_ONLY_USER;
	public static final String EXPERIMENT_CREATE = Roles.USER;
	public static final String EXPERIMENT_EDIT = Roles.USER;
	public static final String EXPERIMENT_DELETE = Roles.USER;
	public static final String EXPERIMENT_MOVE = Roles.USER;
	public static final String EXPERIMENT_ARCHIVE = Roles.USER;
	public static final String EXPERIMENT_EDIT_REMARK = Roles.USER;

	public static final String PLATE_OPEN = Roles.READ_ONLY_USER;
	public static final String PLATE_CREATE = Roles.USER;
	public static final String PLATE_EDIT = Roles.USER;
	public static final String PLATE_DELETE = Roles.USER;
	public static final String PLATE_MOVE = Roles.USER;
	public static final String PLATE_CALCULATE = Roles.USER;
	public static final String PLATE_CHANGE_VALIDATION = Roles.USER;
	public static final String PLATE_CHANGE_APPROVAL = Roles.DATA_MANAGER;
	
	public static final String REPORT_TEMPLATE_CREATE = Roles.ADMINISTRATOR;
	public static final String REPORT_TEMPLATE_EDIT = Roles.ADMINISTRATOR;
	public static final String REPORT_TEMPLATE_DELETE = Roles.ADMINISTRATOR;

	public static final String COMPOUND_CHANGE_VALIDATION = PLATE_CHANGE_VALIDATION;
	public static final String WELL_CHANGE_STATUS = PLATE_EDIT;
}
