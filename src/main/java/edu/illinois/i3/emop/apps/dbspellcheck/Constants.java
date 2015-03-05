package edu.illinois.i3.emop.apps.dbspellcheck;

public class Constants {

	public static final String DB_DRIVER_CLASS = "com.mysql.jdbc.Driver";
	public static final int BONECP_PARTITION_COUNT = 2;
	public static final int BONECP_MIN_CONN_PER_PART = 2;
	public static final int BONECP_MAX_CONN_PER_PART = 10;

	public static final String TABLE_DICT = "dictionary";
	public static final String TABLE_DICT_2GRAMS = "dict_2grams";
	public static final String TABLE_DICT_3GRAMS = "dict_3grams";

	public static final String TABLE_MISSPELLINGS = "misspellings";
	public static final String TABLE_SUGGESTIONS = "suggestions";

}
