package edu.illinois.i3.emop.apps.dbspellcheck;

import com.google.common.base.Objects;

/**
 * @author capitanu
 */
public class Score implements Comparable<Score> {
    private Integer _levenshteinScore;
    private Double _bigramScore;
    private Double _trigramScore;

    public Double getBigramScore() {
        return _bigramScore;
    }

    public void setBigramScore(Double bigramScore) {
        _bigramScore = bigramScore;
    }

    public Double getTrigramScore() {
        return _trigramScore;
    }

    public void setTrigramScore(Double trigramScore) {
        _trigramScore = trigramScore;
    }

    public Integer getLevenshteinScore() {
        return _levenshteinScore;
    }

    public void setLevenshteinScore(Integer levenshteinScore) {
        _levenshteinScore = levenshteinScore;
    }

    public Double getAggregateScore() {
        return Double.valueOf(_levenshteinScore);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("levenstein", _levenshteinScore)
                .add("2gram", _bigramScore)
                .add("3gram", _trigramScore)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Score score = (Score) o;

        if (_bigramScore != null ? !_bigramScore.equals(score._bigramScore) : score._bigramScore != null) return false;
        if (_levenshteinScore != null ? !_levenshteinScore.equals(score._levenshteinScore) : score._levenshteinScore != null)
            return false;
        if (_trigramScore != null ? !_trigramScore.equals(score._trigramScore) : score._trigramScore != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _levenshteinScore != null ? _levenshteinScore.hashCode() : 0;
        result = 31 * result + (_bigramScore != null ? _bigramScore.hashCode() : 0);
        result = 31 * result + (_trigramScore != null ? _trigramScore.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Score other) {
        return getAggregateScore().compareTo(other.getAggregateScore());
    }
}
