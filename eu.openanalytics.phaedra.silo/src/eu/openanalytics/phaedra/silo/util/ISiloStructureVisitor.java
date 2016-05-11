package eu.openanalytics.phaedra.silo.util;

/**
 * <p>
 * A SiloStructure is a hierarchical object that represents the structure of a silo.
 * To navigate a SiloStructure more easily, an ISiloStructureVisitor can be used:
 * </p>
 * <ol>
 * <li>Instantiate an ISiloStructureVisitor</li>
 * <li>Call SiloStructure.receive(visitor)</li>
 * <li>The visit(structure) method will be called automatically for each element (group or dataset) in the structure.</li>
 * </ol>
 */
public interface ISiloStructureVisitor {

	public void visit(SiloStructure structure);

}
