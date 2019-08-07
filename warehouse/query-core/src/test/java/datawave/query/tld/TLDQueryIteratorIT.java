package datawave.query.tld;

import datawave.query.Constants;
import datawave.query.iterator.QueryIteratorIT;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Anything QueryIterator does TLDQueryIterator should do too... plus stuff
 */
public class TLDQueryIteratorIT extends QueryIteratorIT {
    
    @Before
    public void setup() throws IOException {
        super.setup();
        iterator = new TLDQueryIterator();
    }
    
    /**
     * Document specific range given expected from RangeParitioner and EVENT_FIELD1 index lookup. ExceededValueThreshold TF FIELD triggers TF index lookup for
     * evaluation.
     *
     * document specific range, tf via TermFrequencyAggregator
     * 
     * @throws IOException
     */
    @Test
    public void tf_exceededValue_leadingWildcard_documentSpecific_tld_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", "123.345.456");
        
        Map.Entry<Key,Map<String,List<String>>> expectedDocument = getBaseExpectedEvent("123.345.456");
        List<String> tfField1Hits = new ArrayList<>();
        tfField1Hits.add("a b c");
        tfField1Hits.add("r");
        expectedDocument.getValue().put("TF_FIELD1", tfField1Hits);
        
        tf_test(seekRange, "EVENT_FIELD1 =='a' && ((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ '.*r'))", expectedDocument,
                        configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    /**
     * Shard range given expected from RangeParitioner and EVENT_FIELD1 index lookup. ExceededValueThreshold TF FIELD triggers TF index lookup for evaluation.
     *
     * fiFullySatisfies query, Create an ivarator for tf
     * 
     * @throws IOException
     */
    @Test
    public void tf_exceededValue_leadingWildcard_shardRange_tld_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", null);
        Map.Entry<Key,Map<String,List<String>>> expectedDocument = getBaseExpectedEvent("123.345.456");
        List<String> tfField1Hits = new ArrayList<>();
        tfField1Hits.add("a b c");
        tfField1Hits.add("r");
        expectedDocument.getValue().put("TF_FIELD1", tfField1Hits);
        
        tf_test(seekRange, "EVENT_FIELD1 =='a' && ((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ '.*r'))", expectedDocument,
                        configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    @Test
    public void tf_exceededValue_negated_leadingWildcard_documentSpecific_tld_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", "123.345.456");
        tf_test(seekRange, "EVENT_FIELD1 =='a' && !((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ '.*z'))",
                        getBaseExpectedEvent("123.345.456"), configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    @Test
    public void tf_exceededValue_negated_leadingWildcard_shardRange_tld_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", null);
        tf_test(seekRange, "EVENT_FIELD1 =='a' && !((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ '.*z'))",
                        getBaseExpectedEvent("123.345.456"), configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    @Test
    public void tf_exceededValue_trailingWildcard_documentSpecific_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", "123.345.456");
        Map.Entry<Key,Map<String,List<String>>> expectedDocument = getBaseExpectedEvent("123.345.456");
        List<String> tfField1Hits = new ArrayList<>();
        tfField1Hits.add("a b c");
        tfField1Hits.add("r");
        expectedDocument.getValue().put("TF_FIELD1", tfField1Hits);
        
        tf_test(seekRange, "EVENT_FIELD1 =='a' && ((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ 'r.*'))", expectedDocument,
                        configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    @Test
    public void tf_exceededValue_trailingWildcard_shardRange_test() throws IOException {
        // build the seek range for a document specific pull
        Range seekRange = getDocumentRange("row", "dataType1", null);
        Map.Entry<Key,Map<String,List<String>>> expectedDocument = getBaseExpectedEvent("123.345.456");
        List<String> tfField1Hits = new ArrayList<>();
        tfField1Hits.add("a b c");
        tfField1Hits.add("r");
        expectedDocument.getValue().put("TF_FIELD1", tfField1Hits);
        
        tf_test(seekRange, "EVENT_FIELD1 =='a' && ((ExceededValueThresholdMarkerJexlNode = true) && (TF_FIELD1 =~ 'r.*'))", expectedDocument,
                        configureTLDTestData(11), Collections.EMPTY_LIST);
    }
    
    @Override
    protected Range getDocumentRange(String row, String dataType, String uid) {
        // not a document range
        if (uid == null) {
            // get the shard range from the super
            return super.getDocumentRange(row, dataType, uid);
        } else {
            return new Range(new Key("row", "dataType1" + Constants.NULL + uid), true, new Key("row", "dataType1" + Constants.NULL + uid
                            + Constants.MAX_UNICODE_STRING), true);
        }
    }
    
    private List<Map.Entry<Key,Value>> configureTLDTestData(long eventTime) {
        List<Map.Entry<Key,Value>> listSource = super.configureTestData(eventTime);
        
        // add some indexed TF fields in a child
        listSource.add(new AbstractMap.SimpleEntry<>(getEvent("row", "TF_FIELD1", "q r s", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD1", "q r s", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD1", "q", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD1", "r", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD1", "s", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD1", "q", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD1", "r", "dataType1", "123.345.456.1", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD1", "s", "dataType1", "123.345.456.1", eventTime), new Value()));
        
        listSource.add(new AbstractMap.SimpleEntry<>(getEvent("row", "TF_FIELD2", "d e f", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD2", "d e f", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD2", "d", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD2", "e", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getFI("row", "TF_FIELD2", "f", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD2", "d", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD2", "e", "dataType1", "123.345.456.2", eventTime), new Value()));
        listSource.add(new AbstractMap.SimpleEntry<>(getTF("row", "TF_FIELD2", "f", "dataType1", "123.345.456.2", eventTime), new Value()));
        
        return listSource;
    }
}
