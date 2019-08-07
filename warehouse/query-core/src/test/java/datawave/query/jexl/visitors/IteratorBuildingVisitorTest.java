package datawave.query.jexl.visitors;

import datawave.query.Constants;
import datawave.query.attributes.Attribute;
import datawave.query.attributes.Document;
import datawave.query.iterator.NestedIterator;
import datawave.query.iterator.SeekableNestedIterator;
import datawave.query.jexl.JexlASTHelper;
import datawave.query.jexl.LiteralRange;
import datawave.query.predicate.TimeFilter;
import datawave.query.util.IteratorToSortedKeyValueIterator;
import datawave.query.util.TypeMetadata;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.commons.jexl2.parser.ASTERNode;
import org.apache.commons.jexl2.parser.ASTJexlScript;
import org.apache.commons.jexl2.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IteratorBuildingVisitorTest {
    @Test
    public void buildLiteralRange_trailingWildcardTest() throws ParseException {
        ASTJexlScript query = JexlASTHelper.parseJexlQuery("FOO =~ 'bar.*'");
        List<ASTERNode> erNodes = JexlASTHelper.getERNodes(query);
        LiteralRange<?> range = IteratorBuildingVisitor.buildLiteralRange(erNodes.get(0));
        
        Assert.assertTrue(range.getLower().equals("bar"));
        Assert.assertTrue(range.isLowerInclusive());
        Assert.assertTrue(range.getUpper().equals("bar" + Constants.MAX_UNICODE_STRING));
        Assert.assertTrue(range.isUpperInclusive());
    }
    
    /**
     * For the sake of index lookups in the IteratorBuildingVisitor, all leading wildcards are full table FI scans since there is no reverse FI index
     * 
     * @throws ParseException
     */
    @Test
    public void buildLiteralRange_leadingWildcardTest() throws ParseException {
        ASTJexlScript query = JexlASTHelper.parseJexlQuery("FOO =~ '.*bar'");
        List<ASTERNode> erNodes = JexlASTHelper.getERNodes(query);
        LiteralRange<?> range = IteratorBuildingVisitor.buildLiteralRange(erNodes.get(0));
        
        Assert.assertTrue(range.getLower().equals(Constants.NULL_BYTE_STRING));
        Assert.assertTrue(range.isLowerInclusive());
        Assert.assertTrue(range.getUpper().equals(Constants.MAX_UNICODE_STRING));
        Assert.assertTrue(range.isUpperInclusive());
    }
    
    @Test
    public void buildLiteralRange_middleWildcardTest() throws ParseException {
        ASTJexlScript query = JexlASTHelper.parseJexlQuery("FOO =~ 'bar.*man'");
        List<ASTERNode> erNodes = JexlASTHelper.getERNodes(query);
        LiteralRange<?> range = IteratorBuildingVisitor.buildLiteralRange(erNodes.get(0));
        
        Assert.assertTrue(range.getLower().equals("bar"));
        Assert.assertTrue(range.isLowerInclusive());
        Assert.assertTrue(range.getUpper().equals("bar" + Constants.MAX_UNICODE_STRING));
        Assert.assertTrue(range.isUpperInclusive());
    }
    
    @Test
    public void buildLiteralRange_phraseTest() throws ParseException {
        ASTJexlScript query = JexlASTHelper.parseJexlQuery("FOO =~ 'barbaz'");
        List<ASTERNode> erNodes = JexlASTHelper.getERNodes(query);
        LiteralRange<?> range = IteratorBuildingVisitor.buildLiteralRange(erNodes.get(0));
        
        Assert.assertTrue(range.getLower().equals("barbaz"));
        Assert.assertTrue(range.isLowerInclusive());
        Assert.assertTrue(range.getUpper().equals("barbaz"));
        Assert.assertTrue(range.isUpperInclusive());
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarkerJexlNode_RangeTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO >= 'e' && FOO <= 'm'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "f" + Constants.NULL + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // must have doc to get tf field values are within the bounds
        // aggregation fields are not set so no document is created
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
        
        List<String> expected = new ArrayList<>();
        expected.add("f");
        Map<String,List<String>> fooMap = new HashMap<>();
        fooMap.put("FOO", expected);
        
        // turn on aggregation and see the document
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, fooMap);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarkerJexlNodeRange_LowerBoundaryTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO >= 'e' && FOO <= 'm'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // must have doc to get tf field values are within the bounds
        // aggregation fields are not set so no document is created
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
        
        List<String> expected = new ArrayList<>();
        expected.add("e");
        Map<String,List<String>> fooMap = new HashMap<>();
        fooMap.put("FOO", expected);
        
        // turn on aggregation and see the document
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, fooMap);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarkerJexlNode_RangeUpperBoundaryTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO >= 'e' && FOO <= 'm'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "m" + Constants.NULL + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // must have doc to get tf field values are within the bounds
        // aggregation fields are not set so no document is created
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
        
        List<String> expected = new ArrayList<>();
        expected.add("m");
        Map<String,List<String>> fooMap = new HashMap<>();
        fooMap.put("FOO", expected);
        
        // turn on aggregation and see the document
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, fooMap);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarkerJexlNode_RangeUpperBoundaryOutsideTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO >= 'e' && FOO <= 'm'))");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "mn" + Constants.NULL
                        + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // create bounded range filter
        // value outside upper bound so no document found
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, null, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarkerJexlNode_RangeLowerBoundaryOutsideTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO >= 'e' && FOO <= 'm'))");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // create bounded range filter
        // value outside lower bound so no document found
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, null, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexTrailingWildcardNoAggregationTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ 'd.*'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        
        Set<String> termFrequencyFields = new HashSet<>();
        termFrequencyFields.add("FOO");
        
        // must have doc to get tf field values that match regex
        // aggregation fields are not set so no document is created
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, false, null, termFrequencyFields, Collections.EMPTY_SET);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexTrailingWildcardAggregatedFieldsTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ 'd.*'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        Map<String,List<String>> expectedDocValues = new HashMap<>();
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("de");
        expectedDocValues.put("FOO", expectedValues);
        
        // must have doc to get tf field values that match regex
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, expectedDocValues);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexTrailingWildcardAggregatedMultipleFieldsTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ 'd.*'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        Map<String,List<String>> expectedDocValues = new HashMap<>();
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("de");
        expectedValues.add("dd");
        expectedDocValues.put("FOO", expectedValues);
        
        // must have doc including trailing values matching the regex
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, expectedDocValues);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexMiddleWildcardTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ 'd.*foo'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "ddfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dzzzzfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        Map<String,List<String>> expectedDocValues = new HashMap<>();
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("ddfoo");
        expectedValues.add("dzzzzfoo");
        expectedDocValues.put("FOO", expectedValues);
        
        // must have doc to get tf field values that match regex
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, expectedDocValues);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexLeadingWildcardTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && ((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ '.*foo'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "ddfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dzzzzfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        Map<String,List<String>> expectedDocValues = new HashMap<>();
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("ddfoo");
        expectedValues.add("dzzzzfoo");
        expectedDocValues.put("FOO", expectedValues);
        
        // leading wildcard match foo values must have doc including those values
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, true, expectedDocValues);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexLeadingWildcardNegationNoHitsTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && !((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ '.*foo'))");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "ddfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dzzzzfoo" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(
                        new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "e" + Constants.NULL + "FOO"), new Value()));
        
        // doc contains the regex so should not be evaluated
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, null, source, false, null);
    }
    
    @Test
    public void visitAnd_ExceededValueThresholdMarker_RegexLeadingWildcardNegationAltHitTest() throws Exception {
        ASTJexlScript script = JexlASTHelper.parseJexlQuery("BAZ == 'woot' && !((ExceededValueThresholdMarkerJexlNode = true) && (FOO =~ '.*foo'))");
        Key hit = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        
        List<Map.Entry<Key,Value>> source = new ArrayList<>();
        source.add(new AbstractMap.SimpleEntry(new Key("row", "fi" + Constants.NULL + "BAZ", "woot" + Constants.NULL + "dataType" + Constants.NULL
                        + "123.345.456"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "cd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "de" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dd" + Constants.NULL
                        + "FOO"), new Value()));
        source.add(new AbstractMap.SimpleEntry(new Key("row", "tf", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + "dzzzz" + Constants.NULL
                        + "FOO"), new Value()));
        
        // empty document because it didn't find the pattern match (.*foo)
        // ultimately the non .*foo entries don't need to be built because the query only cares if they exist
        // however should be evaluated as a hit since the regex as NOT hit
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(script, hit, source, false, null);
    }
    
    private void vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(ASTJexlScript query, Key docKeyHit, List<Map.Entry<Key,Value>> source,
                    boolean buildDoc, Map<String,List<String>> docKeys) throws Exception {
        Set<String> termFrequencyFields = new HashSet();
        termFrequencyFields.add("FOO");
        
        vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(query, docKeyHit, source, buildDoc, docKeys, termFrequencyFields, termFrequencyFields);
    }
    
    private void vistAnd_ExceededValueThesholdMarkerJexlNode_termFrequencyTest(ASTJexlScript query, Key docKeyHit, List<Map.Entry<Key,Value>> source,
                    boolean buildDoc, Map<String,List<String>> docKeys, Set<String> termFrequencyFields, Set<String> aggregationFields) throws Exception {
        Key startRangeKey = new Key("row", "dataType" + Constants.NULL + "123.345.456");
        Key endRangeKey = new Key("row", "dataType" + Constants.NULL + "123.345.456" + Constants.NULL + Constants.MAX_UNICODE_STRING);
        Range docRange = new Range(startRangeKey, true, endRangeKey, true);
        
        eval(query, docRange, docKeyHit, source, buildDoc, docKeys, termFrequencyFields, aggregationFields);
    }
    
    private void eval(ASTJexlScript query, Range docRange, Key docKeyHit, List<Map.Entry<Key,Value>> source, boolean buildDoc,
                    Map<String,List<String>> docKeys, Set<String> termFrequencyFields, Set<String> aggregationFields) throws IOException {
        IteratorBuildingVisitor visitor = new IteratorBuildingVisitor();
        TypeMetadata typeMetadata = new TypeMetadata();
        
        Iterator<Map.Entry<Key,Value>> iterator = source.iterator();
        visitor.setSource(new SourceFactory(iterator), new BaseIteratorEnvironment());
        
        // configure the visitor for use
        visitor.setTermFrequencyFields(termFrequencyFields);
        visitor.setFieldsToAggregate(aggregationFields);
        visitor.setRange(docRange);
        visitor.setTimeFilter(TimeFilter.alwaysTrue());
        visitor.setLimitLookup(true);
        visitor.setTypeMetadata(typeMetadata);
        
        query.jjtAccept(visitor, null);
        NestedIterator result = visitor.root();
        Assert.assertTrue(result != null);
        SeekableNestedIterator seekableNestedIterator = new SeekableNestedIterator(result);
        seekableNestedIterator.seek(docRange, null, true);
        seekableNestedIterator.initialize();
        
        // asserts for a hit or miss
        if (docKeyHit == null) {
            Assert.assertFalse(seekableNestedIterator.hasNext());
        } else {
            Assert.assertTrue(seekableNestedIterator.hasNext());
            Key next = (Key) seekableNestedIterator.next();
            Assert.assertTrue(next != null);
            Assert.assertTrue(next.getRow().toString().equals(docKeyHit.getRow().toString()));
            Assert.assertTrue(next.getColumnFamily().toString().equals(docKeyHit.getColumnFamily().toString()));
            
            // asserts for document build
            Document d = seekableNestedIterator.document();
            Assert.assertTrue(d != null);
            
            if (buildDoc) {
                // +1 is for RECORD_ID field
                Assert.assertTrue(docKeys.keySet().size() + 1 == d.getDictionary().size());
                
                // verify hits for each specified field
                for (String field : docKeys.keySet()) {
                    List<String> expected = docKeys.get(field);
                    if (expected.size() == 1) {
                        // verify the only doc
                        Assert.assertTrue(d.getDictionary().get(field).getData().equals(expected.get(0)));
                    } else {
                        // the data should be a set, verify it matches expected
                        Object dictData = d.getDictionary().get(field).getData();
                        Assert.assertTrue(dictData != null);
                        Assert.assertTrue(dictData instanceof Set);
                        Set dictSet = (Set) dictData;
                        Assert.assertTrue(dictSet.size() == expected.size());
                        Iterator<Attribute> dictIterator = dictSet.iterator();
                        while (dictIterator.hasNext()) {
                            Assert.assertTrue(expected.remove(dictIterator.next().getData()));
                        }
                        // verify that the expected set is now empty
                        Assert.assertTrue(expected.size() == 0);
                    }
                }
            } else {
                // doc should be empty
                Assert.assertTrue(d.getDictionary().size() == 0);
            }
            
            // there should be no other hits
            Assert.assertFalse(seekableNestedIterator.hasNext());
        }
    }
    
    private static class SourceFactory implements datawave.query.iterator.SourceFactory<Key,Value> {
        private Iterator<Map.Entry<Key,Value>> iterator;
        
        public SourceFactory(Iterator<Map.Entry<Key,Value>> iterator) {
            this.iterator = iterator;
        }
        
        @Override
        public SortedKeyValueIterator<Key,Value> getSourceDeepCopy() {
            return new IteratorToSortedKeyValueIterator(iterator);
        }
    }
}
