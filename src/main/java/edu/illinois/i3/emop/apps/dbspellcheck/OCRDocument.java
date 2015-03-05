package edu.illinois.i3.emop.apps.dbspellcheck;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OCRDocument {

	private final String _name;
	private final Iterator<String> _tokenIterator;
	private final Map<Misspelling, Integer> _misspellings;

	public OCRDocument(String name, Iterator<String> tokenIterator) {
		_name = name;
		_tokenIterator = tokenIterator;
		_misspellings = new HashMap<Misspelling, Integer>();
	}

	public String getName() {
		return _name;
	}

	public Iterator<String> getTokenIterator() {
		return _tokenIterator;
	}

	public Set<Misspelling> getMisspellings() {
		return _misspellings.keySet();
	}

	public Map<Misspelling, Integer> getMisspellingCounts() {
		return _misspellings;
	}

	public int addMisspelling(Misspelling misspelling) {
		Integer count = _misspellings.get(misspelling);
		if (count == null)
			count = 0;

		_misspellings.put(misspelling, ++count);

		return count;
	}
}
