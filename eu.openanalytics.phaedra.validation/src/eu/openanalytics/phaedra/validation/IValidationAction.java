package eu.openanalytics.phaedra.validation;


public interface IValidationAction {

	/**
	 * Execute this action on the specified object(s).
	 * 
	 * @param remark A remark to add to the action.
	 * @param objects The objects to execute the action on.
	 * @throws ValidationException If the action fails for any reason.
	 */
	public void run(String remark, Object... objects) throws ValidationException;
}
