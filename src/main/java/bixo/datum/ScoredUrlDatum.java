package bixo.datum;

import java.util.Arrays;
import java.util.Map;

import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

@SuppressWarnings("serial")
public class ScoredUrlDatum extends GroupedUrlDatum {
    private double _score;

    // Constructor for URL that has never been fetched and has no score.
    public ScoredUrlDatum(String url) {
        this(url, 0, 0, UrlStatus.UNFETCHED, null, 1.0, null);
    }
    
    @SuppressWarnings("unchecked")
    public ScoredUrlDatum(String url, long lastFetched, long lastUpdated, UrlStatus lastStatus, String groupKey, double score, Map<String, Comparable> metaData) {
        super(url, lastFetched, lastUpdated, lastStatus, groupKey, metaData);
        _score = score;
    }

    public double getScore() {
        return _score;
    }

    public void setScore(double score) {
        _score = score;
    }

    // ======================================================================================
    // Below here is all Cascading-specific implementation
    // ======================================================================================
    
    // Cascading field names that correspond to the datum fields.
    public static final String SCORE_FIELD = fieldName(ScoredUrlDatum.class, "score");
        
    public static final Fields FIELDS = GroupedUrlDatum.FIELDS.append(new Fields(SCORE_FIELD));
    
    public ScoredUrlDatum(Tuple tuple, Fields metaDataFields) {
        super(tuple, metaDataFields);
        
        TupleEntry entry = new TupleEntry(getStandardFields(), tuple);
        _score = entry.getDouble(SCORE_FIELD);
    };
    
    
    @Override
    public Fields getStandardFields() {
        return FIELDS;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Comparable[] getStandardValues() {
        Comparable[] baseValues = super.getStandardValues();
        Comparable[] copyOf = Arrays.copyOf(baseValues, baseValues.length + 1);
        copyOf[baseValues.length] = _score;
        return copyOf;
    }

}
