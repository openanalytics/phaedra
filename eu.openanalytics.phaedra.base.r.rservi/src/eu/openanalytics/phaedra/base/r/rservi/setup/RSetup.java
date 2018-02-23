/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package eu.openanalytics.phaedra.base.r.rservi.setup;

import java.util.ArrayList;
import java.util.List;


/**
 * A single R setup loaded or created for a given OS/architecture.
 */
public class RSetup {
	
	
	private final String id;
	
	private String name;
	private String version;
	
	private String os;
	private String arch;
	
	private String rHome;
	
	private final List<String> rLibsSite = new ArrayList<String>(2);
	private final List<String> rLibs = new ArrayList<String>(2);
	private final List<String> rLibsUser = new ArrayList<String>(2);
	
	
	public RSetup(final String id) {
		this.id = id;
	}
	
	
	/**
	 * Returns the unique id of the setup.
	 * 
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Sets the name of the setup.
	 * 
	 * @param the new name
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * Returns the name of the setup.
	 * 
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Sets the R version.
	 * 
	 * @param the new name
	 */
	public void setRVersion(final String version) {
		this.version = version;
	}
	
	/**
	 * Returns the R version of the setup.
	 * 
	 * @return the name
	 */
	public String getRVersion() {
		return this.version;
	}
	
	/**
	 * Sets the operation system and its architecture the setup is loaded or created for.
	 * 
	 * @param os the os constant
	 * @param arch the arch constant
	 */
	public void setOS(final String os, final String arch) {
		this.os = os;
		this.arch = arch;
	}
	
	/**
	 * Returns the operation system the setup is loaded for.
	 * 
	 * @return the os constant
	 */
	public String getOS() {
		return this.os;
	}
	
	/**
	 * Returns the system architecture the setup is loaded for.
	 * 
	 * @return the arch constant
	 */
	public String getOSArch() {
		return this.arch;
	}
	
	
	/**
	 * Sets the R home directory (R_HOME) for the setup.
	 * 
	 * @param the R home directory
	 */
	public void setRHome(final String directory) {
		this.rHome = directory;
	}
	
	/**
	 * Returns the R home directory (R_HOME) for the loaded setup.
	 * 
	 * @return the R home directory
	 */
	public String getRHome() {
		return this.rHome;
	}
	
	
	/**
	 * Returns the library directories for the R_LIBS_SITE group.
	 * 
	 * @return the directories of the libraries
	 */
	public List<String> getRLibsSite() {
		return this.rLibsSite;
	}
	
	/**
	 * Returns the library directories for the R_LIBS group.
	 * 
	 * @return the directories of the libraries
	 */
	public List<String> getRLibs() {
		return this.rLibs;
	}
	
	/**
	 * Returns the library directories for the R_LIBS_USER group.
	 * 
	 * @return the directories of the libraries
	 */
	public List<String> getRLibsUser() {
		return this.rLibsUser;
	}
	
}
