package eu.openanalytics.phaedra.base.util.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.util.BundleUtility;
import org.osgi.framework.Bundle;

/**
 * A collection of utilities related to file manipulation.
 * Note that these methods work on regular files. For file server manipulation,
 * see {@link eu.openanalytics.phaedra.base.environment.IEnvironment#getFileServer}.
 */
@SuppressWarnings("restriction")
public class FileUtils {

	public final static char[] ILLEGAL_FILENAME_CHARS = { '/','?','<','>','\\',':','*','|','"'};

	/**
	 * Extract the name of a file or directory from its full path.
	 * 
	 * @param path The full path of the file or directory.
	 * @return The name of the file or directory denoted by this path.
	 */
	public static String getName(String path) {
		if (path == null) return null;
		String safePath = path.replace('\\', '/');
		int lastSlashIndex = safePath.lastIndexOf("/");
		if (lastSlashIndex == -1) return null;
		return path.substring(lastSlashIndex + 1);
	}

	/**
	 * Extract the path of the file or directory denoted by the file argument.
	 * 
	 * @param path The path of a file or folder whose parent path must be found.
	 * @return The path name, or null if the argument did not represent a full path.
	 */
	public static String getPath(String path) {
		String safePath = path.replace('\\', '/');
		int lastSlashIndex = safePath.lastIndexOf("/");
		if (lastSlashIndex == -1) return null;
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
	 * If the name has no extension, an empty string is returned.
	 * 
	 * @param name The name of the file.
	 * @return The extension of the file.
	 */
	public static String getExtension(String name) {
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex == -1) return "";
		return name.substring(dotIndex + 1);
	}

	/**
	 * Check if a filename has a given extension, ignoring case.
	 * 
	 * @param name The name of the file.
	 * @param ext The extension to match.
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
	 */
	public static String getHumanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Check if a filename contains only valid file name characters.
	 * For example, slashes and question marks are not allowed on many operating systems.
	 * 
	 * @param fileName The name to check.
	 * @return True if the string is a valid file name.
	 */
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
	
	/**
	 * Recursively delete a file or folder. (use this to delete non-empty folders)
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
	
	/**
	 * Recursively delete a file or folder. (use this to delete non-empty folders)
	 */
	public static void deleteRecursive(String dir) {
		deleteRecursive(new File(dir));
	}

	/**
	 * Copy the contents of a file to another location.
	 * If the destination file's path does not exist, it will be created.
	 */
	public static void copy(String filePathFrom, String filePathTo) throws IOException {
		File source = new File(filePathFrom);
		long inputLength = source.length();
		
		File dest = new File(filePathTo);
		dest.getParentFile().mkdirs();
		
		InputStream in = new FileInputStream(filePathFrom);
		OutputStream out = new FileOutputStream(filePathTo);
		
		StreamUtils.copyAndClose(in, out, inputLength, new NullProgressMonitor());
	}
	
	/**
	 * Write an array of bytes into a file.
	 * 
	 * @param bytes The bytes to write.
	 * @param path The location of the file to write to. Parent directory must exist.
	 * @param append True if the bytes should be added to the end of the file.
	 * @throws IOException If the write fails.
	 */
	public static void write(byte[] bytes, String path, boolean append) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		OutputStream out = new FileOutputStream(path, append);
		StreamUtils.copyAndClose(in, out);
	}

	/**
	 * Create a String containing the current year and the current week, formatted as follows:
	 * <p>2011/35</p>
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
	 * @param create True to actually create the folder, false to just create a path String.
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
}
