package edu.illinois.i3.emop.apps.dbspellcheck;

import static edu.illinois.i3.emop.utils.DBUtils.releaseConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import edu.illinois.i3.spellcheck.engine.SpellDictionaryHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

import edu.illinois.i3.emop.utils.DBUtils;

/**
 * Application entry point
 *
 * @author capitanu
 *
 */

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		JSAPResult cmdLine = parseArguments(args);

		String dbUrl = cmdLine.getString("dbUrl");
		String dbUser = cmdLine.getString("dbUser");
		String dbPasswd = cmdLine.getString("dbPasswd");

		File directory = cmdLine.getFile("directory");
		log.info("Using data directory: {}", directory);

		File rulesFile = cmdLine.getFile("transformations");
		log.info("Using rules file: {}", rulesFile);

		BoneCP connectionPool = DBUtils.createDBConnectionPool(Constants.DB_DRIVER_CLASS, dbUrl, dbUser, dbPasswd);
		SpellDictionary dictionary = getDictionary(connectionPool);
		Map<String, Integer> bigramCounts = getNGramCounts(2, connectionPool);
		Map<String, Integer> trigramCounts = getNGramCounts(3, connectionPool);

		setupDatabase(connectionPool);

		InputStream rulesStream = new FileInputStream(rulesFile);
		Map<String, Set<String>> ocrRules = getTransformationRules(rulesStream);

		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String fileName) {
				return fileName.toLowerCase().endsWith(".xml");
			}
		});

        Multiset<Transformation> transformCounts = HashMultiset.create();

        for (File file : files) {
			log.info("Processing file: " + file);
			InputStream documentStream = new FileInputStream(file);
			OCRXMLTokenIterator tokenIterator = new OCRXMLTokenIterator(documentStream);
			OCRDocument document = new OCRDocument(file.getName(), tokenIterator);
			OCRCheck ocrCheck = new OCRCheck(document, connectionPool);
			ocrCheck.setBigramCounts(bigramCounts);
			ocrCheck.setTrigramCounts(trigramCounts);
			ocrCheck.processDocument(dictionary, ocrRules);

            log.info("Updating applied transformations counts...");
            for (Misspelling misspelling : document.getMisspellings())
                for (Suggestion suggestion : misspelling.getSuggestions())
                    transformCounts.addAll(suggestion.getTransformations());
		}

        for (Transformation transformation : Multisets.copyHighestCountFirst(transformCounts).elementSet())
            log.info("{}: {}", transformation, transformCounts.count(transformation));

		log.info("Finished");
	}

	/**
	 * Reads the transformation rules from a stream and constructs a dictionary mapping the replacement rules
     *
	 * @param rulesStream The stream containing the transformation rules
	 * @return A map where the keys represent the OCR errors, and the values are the possible corrections (transformations)
	 * @throws IOException
	 * @throws JSONException
	 */
	private static Map<String, Set<String>> getTransformationRules(InputStream rulesStream) throws IOException, JSONException {
		Map<String, Set<String>> transformRules = new HashMap<String, Set<String>>();
		Reader reader = new BufferedReader(new InputStreamReader(rulesStream));

		try {
			JSONTokener tokener = new JSONTokener(reader);
			JSONObject jsonRules = new JSONObject(tokener);
			@SuppressWarnings("unchecked")
			Iterator<String> keys = jsonRules.keys();

			while (keys.hasNext()) {
				String key = keys.next();  				// key is the "correct" token
				Object value = jsonRules.get(key);		// value is the value (or set of values) representing OCR errors

				JSONArray ocrErrors = (value instanceof JSONArray) ?
						(JSONArray) value : new JSONArray().put(value);

				for (int i = 0, iMax = ocrErrors.length(); i < iMax; i++) {
					String ocrErr = ocrErrors.getString(i);
					Set<String> replacements = transformRules.get(ocrErr);
					if (replacements == null) {
						replacements = new HashSet<String>();
						transformRules.put(ocrErr, replacements);
					}

					replacements.add(key);
				}
			}

			return transformRules;
		}
		finally {
			reader.close();
		}
	}

	private static void setupDatabase(BoneCP connectionPool) throws SQLException {
		Connection connection = null;
		Statement stmt = null;

		try {
			connection = connectionPool.getConnection();
			stmt = connection.createStatement();

			String sqlCreateTableMisspellings = String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"   id INT UNSIGNED NOT NULL AUTO_INCREMENT, " +
					"   misspelling VARCHAR(50) NOT NULL, " +
					"   2gram_score FLOAT NOT NULL, " +
					"   3gram_score FLOAT NOT NULL " +
					")", Constants.TABLE_MISSPELLINGS);


		}
		finally {
			releaseConnection(connection, stmt);
		}

	}

	private static Map<String, Integer> getNGramCounts(int n, BoneCP connectionPool) throws SQLException {
		Connection connection = null;
		Statement stmt = null;

		try {
			connection = connectionPool.getConnection();
			stmt = connection.createStatement();

			log.debug("Loading {}-gram counts...", n);

			String tableName = (n == 2) ? Constants.TABLE_DICT_2GRAMS : Constants.TABLE_DICT_3GRAMS;
			String sqlQueryNgramCounts = String.format("SELECT ngram, count FROM %s", tableName);
			ResultSet rs = stmt.executeQuery(sqlQueryNgramCounts);

			Map<String, Integer> ngramCounts = new HashMap<String, Integer>();

			int sum = 0;
			while (rs.next()) {
				String ngram = rs.getString("ngram");
				int count = rs.getInt("count");
				ngramCounts.put(ngram, count);

				sum += count;
			}

			log.info(String.format("Loaded %,d %d-grams", ngramCounts.size(), n));

			// Store the sum of the counts using the special key "+"
			ngramCounts.put("+", sum);

			return ngramCounts;
		}
		finally {
			releaseConnection(connection, stmt);
		}
	}

	private static SpellDictionary getDictionary(BoneCP connectionPool) throws SQLException {
		Connection connection = null;
		Statement stmt = null;

		try {
			connection = connectionPool.getConnection();
			stmt = connection.createStatement();

			log.debug("Loading dictionary...");

			String sqlQueryWords = String.format("SELECT word FROM %s", Constants.TABLE_DICT);
			ResultSet rs = stmt.executeQuery(sqlQueryWords);

            SpellDictionary dictionary = null;
            try {
                dictionary = new SpellDictionaryHashMap();
                int wordCount = 0;

                while (rs.next()) {
                    dictionary.addWord(rs.getString(1));
                    wordCount++;
                }

                log.info(String.format("Loaded %,d dictionary words", wordCount));
            }
            catch (IOException ex) { }

            return dictionary;
        }
		finally {
			releaseConnection(connection, stmt);
		}
	}

	private static Parameter[] getApplicationParameters() {
		Parameter dbUrl = new FlaggedOption("dbUrl")
								.setDefault("jdbc:mysql://localhost/emop")
								.setLongFlag("dburl")
								.setHelp("DB identifier URL to use");

		Parameter dbUser = new FlaggedOption("dbUser")
								.setDefault("emop")
								.setShortFlag('u')
								.setHelp("DB user to use");

		Parameter dbPasswd = new FlaggedOption("dbPasswd")
								.setRequired(true)
								.setShortFlag('p')
								.setHelp("DB password to use");

		Parameter directory = new FlaggedOption("directory")
								.setStringParser(
										FileStringParser.getParser()
											.setMustBeDirectory(true)
											.setMustExist(true))
								.setRequired(true)
								.setShortFlag('d')
								.setHelp("Directory containing the files to process");

		Parameter transformations = new FlaggedOption("transformations")
								.setStringParser(
										FileStringParser.getParser()
											.setMustBeFile(true)
											.setMustExist(true))
								.setRequired(true)
								.setShortFlag('t')
								.setHelp("The transformation rules to use for OCR correction");

		return new Parameter[] { dbUrl, dbUser, dbPasswd, directory, transformations };
	}

	private static String getApplicationHelp() {
		return "Tokenizes and spellchecks a set of input files from a given directory, recording statistics of the results in the DB";
	}

	private static JSAPResult parseArguments(String[] args) throws JSAPException {
		SimpleJSAP jsap = new SimpleJSAP("DBSpellCheck", getApplicationHelp(), getApplicationParameters());
		JSAPResult result = jsap.parse(args);

		if (jsap.messagePrinted())
			System.exit(1);

		return result;
	}
}
