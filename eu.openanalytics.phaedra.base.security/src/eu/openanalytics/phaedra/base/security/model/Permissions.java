package eu.openanalytics.phaedra.base.security.model;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Permissions are mappings between operations (such as creating or
 * editing an object of a specific type) and the role required
 * to perform that operation.
 */
public class Permissions {
	
	public static class Operation {
		
		private final String id;
		private final String defaultRequiredRole;
		
		Operation(String id, String defaultRequiredRole) {
			if (id == null || defaultRequiredRole == null) throw new NullPointerException();
			this.id = id;
			this.defaultRequiredRole = defaultRequiredRole;
		}
		
		public String getId() {
			return id;
		}
		
	}
	
	public static final Operation PROTOCOLCLASS_OPEN = new Operation("protocolclass.open", Roles.READ_ONLY_USER);
	public static final Operation PROTOCOLCLASS_CREATE = new Operation("protocolclass.create", Roles.USER);
	public static final Operation PROTOCOLCLASS_EDIT = new Operation("protocolclass.edit", Roles.DATA_MANAGER);
	public static final Operation PROTOCOLCLASS_DELETE = new Operation("protocolclass.delete", Roles.DATA_MANAGER);
	
	public static final Operation PROTOCOL_OPEN = new Operation("protocol.open", Roles.READ_ONLY_USER);
	public static final Operation PROTOCOL_CREATE = new Operation("protocol.create", Roles.USER);
	public static final Operation PROTOCOL_EDIT = new Operation("protocol.edit", Roles.USER);
	public static final Operation PROTOCOL_DELETE = new Operation("protocol.delete", Roles.DATA_MANAGER);
	
	public static final Operation EXPERIMENT_OPEN = new Operation("experiment.open", Roles.READ_ONLY_USER);
	public static final Operation EXPERIMENT_CREATE = new Operation("experiment.create", Roles.USER);
	public static final Operation EXPERIMENT_EDIT = new Operation("experiment_edit", Roles.USER);
	public static final Operation EXPERIMENT_DELETE = new Operation("experiment.delete", Roles.USER);
	public static final Operation EXPERIMENT_MOVE = new Operation("experiment.move", Roles.USER);
	public static final Operation EXPERIMENT_ARCHIVE = new Operation("experiment.archive", Roles.USER);
	public static final Operation EXPERIMENT_EDIT_REMARK = new Operation("experiment.edit.remark", Roles.USER);
	
	public static final Operation PLATE_OPEN = new Operation("plate.open", Roles.READ_ONLY_USER);
	public static final Operation PLATE_CREATE = new Operation("plate.create", Roles.USER);
	public static final Operation PLATE_EDIT = new Operation("plate.edit", Roles.USER);
	public static final Operation PLATE_DELETE = new Operation("plate.delete", Roles.USER);
	public static final Operation PLATE_MOVE = new Operation("plate.move", Roles.USER);
	public static final Operation PLATE_CALCULATE = new Operation("plate.calculate", Roles.USER);
	public static final Operation PLATE_CHANGE_VALIDATION = new Operation("plate.change.validation", Roles.USER);
	public static final Operation PLATE_CHANGE_APPROVAL = new Operation("plate.change.approval", Roles.DATA_MANAGER);
	
	public static final Operation COMPOUND_CHANGE_VALIDATION = PLATE_CHANGE_VALIDATION;
	public static final Operation WELL_CHANGE_STATUS = PLATE_EDIT;
	
	
	private static final Operation[] BASE_OPERATIONS = new Operation[] {
			PROTOCOLCLASS_OPEN, PROTOCOLCLASS_CREATE, PROTOCOLCLASS_EDIT, PROTOCOLCLASS_DELETE,
			PROTOCOL_OPEN, PROTOCOL_CREATE, PROTOCOL_EDIT, PROTOCOL_DELETE,
			EXPERIMENT_OPEN, EXPERIMENT_CREATE, EXPERIMENT_EDIT, EXPERIMENT_DELETE,
			EXPERIMENT_MOVE, EXPERIMENT_ARCHIVE, EXPERIMENT_EDIT_REMARK,
			PLATE_OPEN, PLATE_CREATE, PLATE_EDIT, PLATE_DELETE,
			PLATE_MOVE, PLATE_CALCULATE, PLATE_CHANGE_VALIDATION, PLATE_CHANGE_APPROVAL
	};
	
	
	private Map<Operation, String> operationsRoles;
	
	
	public Permissions() {
		operationsRoles = new IdentityHashMap<>();
		
		for (Operation operation : BASE_OPERATIONS) {
			operationsRoles.put(operation, operation.defaultRequiredRole);
		}
	}
	
	public void load(Function<String, String> resolver) {
		for (Map.Entry<Operation, String> entry : operationsRoles.entrySet()) {
			String role = resolver.apply(entry.getKey().getId() + ".req.role");
			if (role != null && !role.isEmpty()) {
				entry.setValue(role);
			}
		}
	}
	
	
	public Set<Operation> getKnownOperations() {
		return Collections.unmodifiableSet(operationsRoles.keySet());
	}
	
	public String getRequiredRole(Operation operation) {
		if (operation == null) throw new NullPointerException();
		return operationsRoles.get(operation);
	}
	
	public void setRequiredRole(Operation operation, String role) {
		if (operation == null || role == null) throw new NullPointerException();
		operationsRoles.put(operation, role);
	}
	
	public void setRequiredRoleToDefault(Operation operation) {
		if (operation == null) throw new NullPointerException();
		operationsRoles.put(operation, operation.defaultRequiredRole);
	}
	
}
