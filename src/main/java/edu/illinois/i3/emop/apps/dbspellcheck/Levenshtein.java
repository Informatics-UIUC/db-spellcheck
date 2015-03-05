package edu.illinois.i3.emop.apps.dbspellcheck;

/**
 * Calculate Levenshtein distance between two strings
 * (code adapted for Java based on http://en.wikipedia.org/wiki/Levenshtein_distance)
 *
 * @author capitanu
 *
 */
public class Levenshtein {

	public static int distance(String source, String target) {
	    // degenerate cases
	    if (source == target) return 0;
	    if (source.length() == 0) return target.length();
	    if (target.length() == 0) return source.length();

	    // create two work vectors of integer distances
	    int[] v0 = new int[target.length() + 1];
	    int[] v1 = new int[target.length() + 1];

	    // initialize v0 (the previous row of distances)
	    // this row is A[0][i]: edit distance for an empty s
	    // the distance is just the number of characters to delete from t
	    for (int i = 0; i < v0.length; i++)
	        v0[i] = i;

	    for (int i = 0; i < source.length(); i++) {
	        // calculate v1 (current row distances) from the previous row v0

	        // first element of v1 is A[i+1][0]
	        //   edit distance is delete (i+1) chars from s to match empty t
	        v1[0] = i + 1;

	        // use formula to fill in the rest of the row
	        for (int j = 0; j < target.length(); j++)
	        {
	            int cost = (source.charAt(i) == target.charAt(j)) ? 0 : 1;
	            v1[j + 1] = Math.min(Math.min(v1[j] + 1, v0[j + 1] + 1), v0[j] + cost);
	        }

	        // copy v1 (current row) to v0 (previous row) for next iteration
	        for (int j = 0; j < v0.length; j++)
	            v0[j] = v1[j];
	    }

	    return v1[target.length()];
	}

}
