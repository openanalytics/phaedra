package eu.openanalytics.phaedra.base.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.util.BundleUtility;
import org.osgi.framework.Bundle;

import eu.openanalytics.phaedra.base.util.misc.RetryingUtils;

@SuppressWarnings("restriction")
public class FileUtils {

	public final static String TAB_DELIMITER = "\t";
	public final static char[] ILLEGAL_FILENAME_CHARS = {
                                 '/','?','<','>','\\',':','*','|','"'};
	public final static char DEFAULT_ESCAPE_CHAR = '_';
	
	/**
	 * Checks for the starting characters and determines whether or not it is a UNC path.
	 * Validity checks aren't done.
	 * 
	 * @param path
	 * @return 
	 */
	public static boolean isUNC(String path){
		if(path.startsWith("\\\\") || path.startsWith("//"))
			return true;
		
		return false;
	}
	
	/**
	 * Extract the name of a file or directory from its full path.
	 * 
	 * @param path
	 *            The full path (containing forward slashes as separators).
	 * @return The name of the file or directory denoted by this path.
	 */
	public static String getName(String path) {
		if (path == null) return null;
		String safePath = path.replace('\\', '/');
		int lastSlashIndex = safePath.lastIndexOf("/");
		if (lastSlashIndex == -1)
			return null;
		return path.substring(lastSlashIndex + 1);
	}

	/**
	 * Extract the path of the file or directory denoted by the file argument.
	 * 
	 * @param path
	 *            The path of a file or folder whose parent path must be found.
	 * @return The path name, or null if the argument did not represent a full
	 *         path.
	 */
	public static String getPath(String path) {
		String safePath = path.replace('\\', '/');
		int lastSlashIndex = safePath.lastIndexOf("/");
		if (lastSlashIndex == -1)
			return null;
		return path.substring(0, lastSlashIndex);
	}

	/**
	 * Convert an absolute file or folder path into a relative path to another path.
	 * 
	 * @param absolutePath The absolute path to convert.
	 * @param relativeTo The path to which the absolute path must be relativized.
	 * @return The converted relative path.
	 */
	public static String getRelativePath(String absolutePath, String relativeTo) {
		Path abs = Paths.get(absolutePath);
		Path relTo = Paths.get(relativeTo);
		Path relative = relTo.relativize(abs);
		return relative.toString();
	}
	
	/**
	 * Extract the extension of a file from its name.
	 * 
	 * @param name
	 *            The name of the file.
	 * @return The extension of the file.
	 */
	public static String getExtension(String name) {
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex == -1)
			return "";
		return name.substring(dotIndex + 1);
	}

	/**
	 * Check if a filename has a given extension, ignoring case.
	 * 
	 * @param name
	 *            The name of the file.
	 * @param ext
	 *            The extension to match.
	 * @return True if the filename ends with the extension.
	 */
	public static boolean hasExtension(String name, String ext) {
		String fileExt = getExtension(name);
		return fileExt.equalsIgnoreCase(ext);
	}

	/**
	 * Get the absolute path for a given path.
	 * If the path is absolute already, it is returned unchanged.
	 *  
	 * @param path The path, either absolute or relative.
	 * @return The absolute version of the path.
	 */
	public static String getAbsolutePath(String path) {
		if (path == null) return null;
		File f = new File(path);
		if (f.isAbsolute()) return path;
		return f.getAbsolutePath();
	}
	
	/**
	 * Returns the byte size in a human readable way.
	 * 
	 * Examples:	SI		Byte Prefix
	 * 1000 bytes:	1kB		1000 B
	 * 1024 bytes:			1 KiB
	 * 
	 * @param bytes The byte size
	 * @param si True follows the International System of Units, False returns the binary prefix
	 * @return
	 */
	public static String getHumanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Escape a file name by replacing illegal characters
	 * with an escape character "_". This is irreversible.
	 * 
	 * @param fileName The original file name to escape.
	 * @return The escaped file name.
	 */
	public static String escape(String fileName) {
		String escaped = fileName;
		for (char invalidChar: ILLEGAL_FILENAME_CHARS) {
			escaped = escaped.replace(invalidChar, DEFAULT_ESCAPE_CHAR);
		}
		return escaped;
	}
	
	public static boolean isValidFilename(String fileName) {
		for (char invalidChar: ILLEGAL_FILENAME_CHARS) {
			if (fileName.contains(""+invalidChar)) return false;
		}
		return true;
	}
	
	/**
	 * Unzip the contents of the specified ZIP file to the specified destination.
	 * Existing files will be overwritten. Directories will be created when needed.
	 * 
	 * @param zipFilePath The ZIP file to unzip.
	 * @param destinationDir The directory to unzip to.
	 * @throws IOException If the unzip operation fails.
	 */
	public static void unzip(String zipFilePath, String destinationDir) throws IOException {
		new File(destinationDir).mkdirs();
		ZipFile zipFile = new ZipFile(zipFilePath);
		try {
			for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
				ZipEntry currentEntry = e.nextElement();
				String name = currentEntry.getName();
				if (currentEntry.isDirectory()) {
					new File(destinationDir + "/" + name).mkdir();
				} else {
					String path = getPath(name);
					if (path != null) {
						new File(destinationDir + "/" + path).mkdirs();
					}
					InputStream in = new BufferedInputStream(zipFile.getInputStream(currentEntry));
					OutputStream out = new FileOutputStream(destinationDir + "/" + name);
					StreamUtils.copyAndClose(in, out);
				}
			}
		} finally {
			zipFile.close();
		}
	}
	
	public static void zip(String directory, String destinationPath) throws IOException {
		zip(directory, destinationPath, true);
	}
	
	public static void zip(String directory, String destinationPath, boolean deflate) throws IOException {
		File parent = new File(destinationPath).getParentFile();
		parent.mkdirs();
		// List files BEFORE creating the ZIP file, in case the ZIP file is inside the directory.
		File[] filesToZip = new File(directory).listFiles();
		OutputStream out = new FileOutputStream(destinationPath);
		ZipOutputStream zipOut = null;
		try {
			zipOut = new ZipOutputStream(out);
			String root = directory;
			for (File fileToZip: filesToZip) {
				zipRecursive(fileToZip, root, zipOut, deflate);
			}
		} finally {
			if (zipOut != null) {
				try { zipOut.close(); } catch (IOException e) {}
			}
		}		
	}

	private static void zipRecursive(File source, String root, ZipOutputStream out, boolean deflate) throws IOException {
		if (source.isFile()) {	
			InputStream in = null;
			try {
				String path = source.getPath().replace('\\', '/');
				in = new FileInputStream(source);
				ZipEntry zipEntry = null;
				if (path.startsWith(root)) {
					zipEntry = new ZipEntry(path.substring(root.length()+1));
				} else {
					// Not the same root -> lose the hierarchy
					zipEntry = new ZipEntry(source.getName());
				}
				
				zipEntry.setTime(source.lastModified());
				if (deflate) {
					zipEntry.setMethod(ZipEntry.DEFLATED);
				} else {
					zipEntry.setSize(source.length());
					zipEntry.setCrc(StreamUtils.calculateCRC(new FileInputStream(source)));
					zipEntry.setMethod(ZipEntry.STORED);	
				}
				out.putNextEntry(zipEntry);	
				StreamUtils.copy(in, out);
			} finally {
				if (in != null) in.close();
			}
		} else {
			File[] children = source.listFiles();
			for (File child: children) {
				zipRecursive(child, root, out, deflate);
			}
		}
	}
	
	/**
	 * Copy a file or folder to a destination folder.
	 * If the source is a folder, all contents will be copied recursively.
	 * 
	 * @param sourcePath The path of the file or folder to copy.
	 * @param destination The path of the folder to copy into.
	 * @throws IOException If the copy fails due to an I/O error.
	 */
	public static void copyRecursive(String sourcePath, String destination) throws IOException {
		File file = new File(sourcePath);
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isFile()) {
			String destPath = destination + "/" + file.getName();
			FileUtils.copy(sourcePath, destPath);
		} else if (file.isDirectory()) {
			String subDestination = destination + "/" + file.getName();
			new File(subDestination).mkdirs();
			File[] children = file.listFiles();
			for (File child : children) {
				copyRecursive(child.getAbsolutePath(), subDestination);
			}
		}
	}
	
	/**
	 * Recursively delete a file or folder. (use this to delete non-empty
	 * folders)
	 * 
	 * @param file
	 *            The file or folder to delete.
	 */
	public static void deleteRecursive(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isFile()) {
			file.delete();
		} else if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (File child : children) {
				deleteRecursive(child);
			}
			file.delete();
		}
	}
	
	public static void deleteRecursive(String dir) {
		deleteRecursive(new File(dir));
	}

	/**
	 * Rename (move) a file or folder.
	 * This method checks that the source exists and that the destination doesn't exist.
	 * It retries the rename several times, in case the rename fails due to e.g. a temporary file lock.
	 * 
	 * @param from The file or folder to rename.
	 * @param to The destination file or folder.
	 * @throws IOException If the rename fails for any reason.
	 */
	public static void rename(String from, String to) throws IOException {
		File fromFile = new File(from);
		File toFile = new File(to);
		if (!fromFile.exists()) throw new IOException("Cannot rename: source does not exist: " + from);
		if (toFile.exists()) throw new IOException("Cannot rename: target already exists: " + to);
		try {
			RetryingUtils.doRetrying(() -> {
				boolean renamed = fromFile.renameTo(toFile);
				if (!renamed) throw new IOException("Rename failed for " + from);
			}, 10, 1000);
		} catch (Exception e) {
			if (e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
	}
	
	/**
	 * Copy the contents of a File to another File. If the destination file's
	 * path does not exist, it will be created.
	 * 
	 * @param filePathFrom
	 *            The path to the file to copy from.
	 * @param filePathTo
	 *            The path of the file to copy to.
	 * @throws IOException
	 *             If an error occurs during the copy.
	 */
	public static void copy(String filePathFrom, String filePathTo)
			throws IOException {
		
		copy(filePathFrom, filePathTo, null);
	}
	
	public static void copyDir(String dirPathFrom, String dirPathTo) throws IOException{
		org.apache.commons.io.FileUtils.copyDirectory(
				new File(dirPathFrom)
				, new File(dirPathTo)
		);
	}
	
	public static void copy(String filePathFrom, String filePathTo, IProgressMonitor monitor)
			throws IOException {
		
		File source = new File(filePathFrom);
		long inputLength = source.length();
		
		File dest = new File(filePathTo);
		dest.getParentFile().mkdirs();
		
		InputStream in = new FileInputStream(filePathFrom);
		OutputStream out = new FileOutputStream(filePathTo);
		
		StreamUtils.copyAndClose(in, out, inputLength, monitor);
	}
	
	/**
	 * Write an array of bytes into a file.
	 * 
	 * @param bytes The bytes to write.
	 * @param path The location of the file to write to. Parent dir must exist.
	 * @param append True if the bytes should be added to the end of the file.
	 * @throws IOException If the write fails.
	 */
	public static void write(byte[] bytes, String path, boolean append) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		OutputStream out = new FileOutputStream(path, append);
		StreamUtils.copyAndClose(in, out);
	}

	/**
	 * Create a String containing the current year and the current week,
	 * formatted as follows:
	 * <p>
	 * 2011/35
	 * </p>
	 * 
	 * @return A year and week based String.
	 */
	public static String createYearWeekString() {
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy/ww");
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(format.format(date));
		return stringBuilder.toString();
	}
	
	/**
	 * Generate a temporary folder under the application directory.
	 * 
	 * @param create True to actually create the folder, false to
	 * just create a path String.
	 * @return A new, empty temp folder.
	 */
	public static String generateTempFolder(boolean create) {
		String tempFolder = getUserTempFolder() + "/" + UUID.randomUUID();
		if (create) new File(tempFolder).mkdirs();
		return tempFolder;
	}
	
	/**
	 * Remove all contents from the temporary folder.
	 * This should only be called upon application exit.
	 */
	public static void clearTempFolder() {
		String tempFolder = getUserTempFolder();
		File[] contents = new File(tempFolder).listFiles();
		if (contents != null) {
			for (File file: contents) {
				deleteRecursive(file);
			}
		}
	}
	
	private static String getUserTempFolder() {
		StringBuilder sb = new StringBuilder();
		sb.append(System.getProperty("java.io.tmpdir"));
		char c = sb.charAt(sb.length()-1);
		if (c != '/' && c != '\\') sb.append("/");
		sb.append("phaedra-temp/");
		sb.append(System.getProperty("user.name"));
		return sb.toString();
	}
	
	/**
	 * Copy a file from the plugin to a specified location.
	 * If the location's directory does not exist, it will be created.
	 * 
	 * @param path The path within the plugin to the file to copy.
	 * @param pluginID The ID of the plugin containing the file.
	 * @param destination The path where the file will be copied to.
	 * @return The location of the file, inside the destination.
	 * @throws IOException If an I/O Error occurs during the copy.
	 */
	public static String copyFromPlugin(String path, String pluginID, String destination) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			Bundle bundle = Platform.getBundle(pluginID);
			if (bundle == null) {
				// Try again for fragment.
				pluginID += ".win32.win32.x86";
				if (System.getProperty("eclipse.commands").contains("x86_64")) {
					pluginID += "_64";
				}
				bundle = Platform.getBundle(pluginID);
			}
			
			URL url = BundleUtility.find(bundle, path);
			URLConnection conn = url.openConnection();
			in = conn.getInputStream();

			File dest = new File(destination);
			dest.mkdirs();

			String destFile = destination + File.separator + getName(path);
			out = new FileOutputStream(destFile);
		
			StreamUtils.copyAndClose(in, out);
			
			return destFile;
		} finally {
			if (in != null) try {in.close();}catch(IOException e) {}
			if (out != null) try {out.close();}catch(IOException e) {}
		}
	}
	
	/**
	 * Obtain the canonical path to a plugin or fragment path.
	 * This makes sense only if the plugin or fragment is exploded on install
	 * (see the feature file).
	 * 
	 * @param pluginId The ID of the plugin or fragment (without fragment filter).
	 * @param path The path within the plugin, starting with a slash.
	 * @param fragment True if the plugin is a fragment.
	 * @return The canonical path to the plugin path requested, or null if not found.
	 */
	public static String getCanonicalPath(String pluginId, String path, boolean fragment) {
		if (fragment) {
			pluginId += ".win32.win32.x86";
			if (System.getProperty("eclipse.commands").contains("x86_64")) {
				pluginId += "_64";
			}
		}
		
		Bundle b = Platform.getBundle(pluginId);
		if (b == null) return null;
		URL intern = b.getEntry(path);
		if (intern == null) return null;
		
		URL java = null;
		try {
			java = FileLocator.resolve(intern);
		} catch (IOException e) {
			return null;
		}
		String canonicalPath = java.toExternalForm();
		if (canonicalPath.startsWith("file:/")) canonicalPath = canonicalPath.substring("file:/".length());
		if (canonicalPath.endsWith("/")) canonicalPath = canonicalPath.substring(0,canonicalPath.length()-1);
		return canonicalPath;
	}

	public static class FilesOnlyFileFilter implements FileFilter {

		private static FilesOnlyFileFilter instance = new FilesOnlyFileFilter();
		
		private FilesOnlyFileFilter () {
			// Private constructor.
		}
		
		public static FilesOnlyFileFilter getInstance () {
			return instance;
		}
		
		public boolean accept(File f) {
			return (f.isFile() && f.exists() && !f.isHidden());
		}

	}

	public static class FileNameComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ModificationDateComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			long result = o1.lastModified() - o2.lastModified();
			if (result < 0) return -1;
			if (result > 0) return 1;
			return 0;
		}
	}
	
	public static void remove(String path) throws IOException {
		org.apache.commons.io.FileUtils.deleteDirectory(new File(path));
	}
	
	public static String resolveMappedPath(String path) throws IOException {
		if(isUNC(path)) return path;
		
		String drive = path.substring(0, 2);
		Runtime rt = Runtime.getRuntime();
		Process process = rt.exec("net use " + drive);
		
		InputStream in = process.getInputStream();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
		
		String line = null;
		String[] components = null;
		while (null != (line = bufferedReader.readLine())) {
			// Split on whitespace; line break, tab, space
			// [0] Remote
			// [2] \\share
			components = line.split("\\s+");
			if(components[0].equals("Remote"))
				path = path.replace(drive, components[2]);
		}	
		return path;
	}
}
