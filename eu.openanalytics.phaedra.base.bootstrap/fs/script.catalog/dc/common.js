/*****************************************************************************************
 * A collection of utility functions for use in datacapture scripts.
 * 
 * To make use of these functions, add the following line to your datacapture script:
 * load("script://dc/common.js");
 * 
 * In addition to the functions below, all datacapture scripts have access to the following objects:
 * ctx		- An instance of eu.openanalytics.phaedra.datacapture.DataCaptureContext
 * task		- An instance of eu.openanalytics.phaedra.datacapture.DataCaptureTask
 * config	- An instance of eu.openanalytics.phaedra.datacapture.config.ModuleConfig
 * 
 *****************************************************************************************/

/**
 * Find the first file matching a pattern.
 * 
 * @param basePath The folder to search in. This may be a relative path, in which case it will be resolved for the current reading's source path.
 * @param filePattern The regex pattern that the file must match.
 * @param optional True to log a warning if the file is not found, false to throw an exception if the file is not found.
 * @returns The path to the matching file, or null if no match was found.
 */
function findFile(basePath, filePattern, optional) {
	var resolvedBasePath = API.get("CaptureUtils").resolvePath(basePath, reading.getSourcePath(), ctx);
	var filePath = API.get("CaptureUtils").getMatchingChild(resolvedBasePath, filePattern, ctx);		
	var isOptional = (optional === true || (typeof optional === "string" && optional.toLowerCase().equals("true")));

	// Verify the existence of the data file.
	if (filePath == null) {
		if (isOptional) {
			ctx.getLogger().warn(reading, "Optional data file missing. Expected file '" + filePattern + "' at location " + resolvedBasePath);
			return null;
		} else {
			var msg = "Data file missing. Expected file '" + filePattern + "' at location " + resolvedBasePath;
			ctx.getLogger().error(reading, msg);
			API.get("CaptureUtils").doError("Reading " + reading.getBarcode() + ": " + msg);
		}
	} else {
		ctx.getLogger().info(reading, "Data file found: " + filePath);
	}
	
	return filePath;
}

/**
 * Find the first folder matching a pattern.
 *
 * @param basePath The folder to search in. This may be a relative path, in which case it will be resolved for the current reading's source path.
 * @param folderPattern The regex pattern that the folder must match.
 * @returns The path to the matching folder, or null if no match was found.
 */
function findFolder(basePath, folderPattern) {
	var resolvedBasePath = API.get("CaptureUtils").resolvePath(basePath, reading.getSourcePath(), ctx);
	var folderPath = API.get("CaptureUtils").getMatchingChild(resolvedBasePath, folderPattern, ctx);	
	if (new java.io.File(folderPath).isDirectory()) return folderPath;
	else return null;
}

/**
 * Find multiple files or folders matching a pattern.
 * 
 * @param basePath The folder to search in. This may be a relative path, in which case it will be resolved for the current reading's source path.
 * @param filePattern The regex pattern that the files must match.
 * @returns An array of paths of matching children, possibly empty.
 */
function findFiles(basePath, filePattern) {
	var resolvedPath = basePath;
	if (typeof reading != "undefined") resolvedPath = API.get("CaptureUtils").resolvePath(basePath, reading.getSourcePath(), ctx);
	var files = API.get("CaptureUtils").getMatchingChildren(resolvedPath, filePattern, ctx);	
	return files;
}

/**
 * Find multiple folders matching a pattern.
 * 
 * @param basePath The folder to search in. This may be a relative path, in which case it will be resolved for the current reading's source path.
 * @param folderPattern The pattern that the folders must match.
 * @returns An array of paths of matching children, possibly empty.
 */
function findFolders(basePath, folderPattern) {
	var contents = findFiles(basePath, folderPattern);
	var matches = new java.util.ArrayList();
	for (var i in contents) {
		if (new java.io.File(contents[i]).isDirectory()) {
			matches.add(contents[i]);
		}
	}
	return matches.toArray();
}

/**
 * In an array of Strings, find the index of the specified String.
 * 
 * @param strings The array of Strings
 * @param string The String to find
 * @param caseSensitive True to compare the string with case sensitivity. Default true.
 * @returns {Number} The index of the String, or -1 if it wasn't found.
 */
function findString(strings, string, caseSensitive) {
	var isCaseSensitive = (caseSensitive === undefined || caseSensitive == true || caseSensitive == "true");
	var index = -1;
	for (var i=0; i<strings.length; i++) {
		if (isCaseSensitive) {
			if (strings[i].equals(string)) index = i;
		} else {
			if (strings[i].equalsIgnoreCase(string)) index = i;
		}
	}
	return index;
}

/**
 * Trim all Strings in an array.
 * 
 * @param strings The array of Strings to trim
 */
function trimStrings(strings) {
	if (strings == null) return;
	for (var i=0; i<strings.length; i++) {
		if (strings[i] != null) strings[i] = strings[i].trim();
	}
}

/**
 * Match a regular expression against a string.
 * 
 * @param string The string to test.
 * @param pattern The regular expression pattern.
 * @param group The number of the group in the pattern to return.
 * @returns The matching substring, or null if no match was found.
 */
function matchPattern(string, pattern, group) {
	return API.get("CaptureUtils").getMatch(string, pattern, group);
}

/**
 * Concatenate an array of Strings, putting a separator between each String.
 * @param strings The Strings to concatenate.
 * @param separator The separator to place in between, or null for no separator.
 * @returns {String} The concatenated String.
 */
function concatStrings(strings, separator) {
	if (separator == null) separator = "";
	var output = "";
	for (var i=0; i<strings.length; i++) {
		output += strings[i] + separator;
	}
	return output;
}

/**
 * For each reading in the current datacapture context, perform a function.
 * 
 * @param func The function to perform. Arguments passed: the reading object and the zero-based reading index.
 */
function forEachReading(func) {
	monitor.beginTask("Processing readings", 100);
	var readings = ctx.getReadings();
	for (var i=0; i<readings.length; i++) {
		if (monitor.isCanceled()) break;
		reading = readings[i];
		ctx.setActiveReading(reading);
		
		//TODO Deprecated, but still used by scripts while resolving parameter values such as file patterns.
		config.getParameters().setParameter("barcode", reading.getBarcode());
		config.getParameters().setParameter("sequence", reading.getFileInfo());
		
		func(reading, i);
		monitor.worked(100/readings.length);
	}
	monitor.done();
}

/**
 * Execute a function on each element in an array. The functions will be executed in parallel via a threadpool.
 * If one fails, the others will be cancelled before an exception is thrown.
 * 
 * Note: Nashorn is not thread-aware, so the function MUST be threadsafe!
 * The following operations WILL cause problems:
 * - Sharing globals or script objects (JS Arrays, dictionaries, objects, ...)
 * - Looking up or instantiating Java classes (java.lang.System, new java.io.File, ...)
 * 
 * For more information, see https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt
 * 
 * @param array The array of elements.
 * @param func The function to execute on each element. 
 */
function forEachMT(array, func) {
	var pool = java.util.concurrent.ForkJoinPool.commonPool();
	var runnableFactory = function(element) {
		return function() {
			func(element);
		};
	};

	var tasks = [];
	for (var i=0; i<array.length; i++) {
		var runnable = runnableFactory(array[i]);
		var task = pool["submit(Runnable)"](runnable);
		tasks.push(task);
	}

	var exception = null;
	for (var i in tasks) {
		try {
			tasks[i].join();
		} catch (e) {
			exception = e;
			for (var j in tasks) tasks[j].cancel(true);			
			break;
		}
	}
	if (exception != null) throw exception;
}

/**
 * Parse a file using the given parser ID.
 * 
 * @param filePath The path of the file to parse.
 * @param parserId The ID of the parser to use.
 * @returns The parsed model.
 */
function parseFile(filePath, parserId) {
	var params = API.get("CaptureUtils").createParamMap(config);
	var model = API.get("ParserService").parse(filePath, parserId, params);
	return model;
}

/**
 * Parse an XML file.
 * 
 * @param filePath The path of the XML file.
 * @returns The parsed DOM document.
 */
function parseXMLFile(filePath) {
	var stream = null;
	var doc = null;
	try {
		stream = new java.io.FileInputStream(filePath);
		doc = API.get("XmlUtils").parse(stream);
	} finally {
		stream.close();
	}
	return doc;
}

/**
 * Parse a CSV file.
 * 
 * @param filePath The path of the CSV file.
 * @returns The parsed contents as a List of String arrays.
 */
function parseCSV(filePath) {
	try {
		var reader = API.get("CaptureUtils").getCSVReader(filePath);
		var rows = reader.readAll();
	} finally {
		if (reader != null) reader.close();
	}
	return rows;
}

/**
 * Read string data from a file.
 * 
 * @param filePath The file to read.
 * @returns The contents of the file, as a string.
 */
function readFile(filePath) {
	var input = API.get("StreamUtils").readAll(filePath);
	return new java.lang.String(input);
}

/**
 * Write string data to a file.
 * 
 * @param filePath The file to write.
 * @param contents The string to write to the file.
 */
function writeFile(filePath, contents) {
	API.get("FileUtils").write(new java.lang.String(contents).getBytes(), filePath, false);
}

/**
 * For each tag matching an XPath query, perform a function.
 * 
 * @param doc The DOM document to query.
 * @param xpath The XPath query.
 * @param func The function to perform. Arguments passed: the matching tag, the zero-based tag index.
 */
function forEachTag(doc, xpath, func) {
	var tags = API.get("XmlUtils").findTags(xpath, doc);
	for (var i=0; i<tags.getLength(); i++) {
		var tag = tags.item(i);
		func(tag, i);
	}
}

/**
 * Save a model in the capture store for the current reading.
 * 
 * @param model The model to save.
 */
function saveModel(model) {
	ctx.getStore(reading).saveModel(model);
}

/**
 * Calculate a square-ish plate size for the given number of wells.
 * 
 * @param wellCount The number of wells that should fit in the plate.
 * @param minColumns Optional, the minimum nr of columns.
 * @param useStandardSize Optional, if true a standard plate size (96, 384, 1536) will be used.
 */
function calculatePlateSize(wellCount, minColumns, useStandardSize) {
	if (!minColumns) minColumns = 1;
	if (useStandardSize) {
		if (wellCount <= 96 && minColumns <= 12) return [ 8, 12 ];
		if (wellCount <= 384 && minColumns <= 24) return [ 16, 24 ];
		return [ 32, 48 ];
	}
	var sqrt = Math.sqrt(wellCount);
	var columns = Math.max(minColumns, Math.ceil(sqrt));
	var rows = Math.ceil(wellCount/columns);
	return [rows, columns];
}

/**
 * From an array of column names, attempt to find the column describing the well nr.
 * If no match is found, returns null.
 * 
 * @param columnNames The names to choose from.
 * @returns The matching name, or null if no match is found.
 */
function guessWellColumn(columnNames) {
	if (columnNames == null) return null;
	var pattern = "(?i)(well|area)[ _\\-]*(id|nr|number|code)?";
	for (var i in columnNames) {
		if (matchPattern(columnNames[i], pattern, 1) != null) return columnNames[i];
	}
	return null;
}

/**
 * Resolve the variables in a string.
 * 
 * @param string The string that may contain variables.
 */
function resolveVars(string) {
	return API.get("CaptureUtils").resolveVars(string, false, ctx);
}

/**
 * Get a parameter value from the lookup order:
 * 1. Reading runtime parameters
 * 2. DataCaptureTask runtime parameters
 * 3. Module parameters
 * 4. DataCaptureTask global parameters
 * 
 * If there is no such parameter, return the defaultValue.
 * 
 * @param name The name of the parameter to get.
 * @param defaultValue The default value to return if the parameter is missing.
 */
function getParameter(name, defaultValue) {
	var value = API.get("CaptureUtils").resolveVar(name, ctx);
	if (value == null) return defaultValue;
	else return value;
}

function getParameterAsObject(name) {
	return Java.type("eu.openanalytics.phaedra.datacapture.util.VariableResolver").get(name, ctx);
}

/**
 * Set a parameter in the shared configuration (accessible by all modules).
 * 
 * @param name The name of the parameter to set.
 * @param value The value to set.
 */
function setParameter(name, value) {
	if (name.startsWith("reading.")) {
		name = name.substring(8);
		ctx.getParameters(reading).setParameter(name, value);
	} else {
		ctx.getTask().getParameters().put(name, value);
	}
}

function doError(message, err) {
	if (typeof reading == "undefined") {
		ctx.getLogger().error(message, err);
		API.get("CaptureUtils").doError(message);
	} else {
		ctx.getLogger().error(reading, message, err);
		API.get("CaptureUtils").doError("Reading " + reading.getBarcode() + ": " + message);
	}
}

status = "Common functions loaded.";