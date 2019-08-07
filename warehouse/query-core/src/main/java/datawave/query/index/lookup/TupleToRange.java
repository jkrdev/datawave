package datawave.query.index.lookup;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import datawave.query.config.ShardQueryConfiguration;
import datawave.query.jexl.visitors.JexlStringBuildingVisitor;
import datawave.query.planner.QueryPlan;
import datawave.query.ranges.RangeFactory;
import datawave.query.util.Tuple2;

import org.apache.accumulo.core.data.Range;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Transforms information from the index into ranges used to search the shard table.
 *
 */
public class TupleToRange implements Function<Tuple2<String,IndexInfo>,Iterator<QueryPlan>> {
    
    private static final Logger log = Logger.getLogger(TupleToRange.class);
    protected JexlNode currentScript;
    protected JexlNode tree = null;
    protected ShardQueryConfiguration config;
    
    /**
     * @param currentNode
     * @param config
     */
    public TupleToRange(JexlNode currentNode, ShardQueryConfiguration config) {
        this.currentScript = currentNode;
        this.config = config;
    }
    
    /**
     * Transform the index information into a QueryPlan by building ranges.
     *
     * @param tuple
     * @return
     */
    public Iterator<QueryPlan> apply(Tuple2<String,IndexInfo> tuple) {
        String shard = tuple.first();
        IndexInfo indexInfo = tuple.second();
        
        JexlNode queryNode = currentScript;
        if (log.isTraceEnabled() && indexInfo.getNode() != null) {
            log.trace("Got it from tuple " + JexlStringBuildingVisitor.buildQuery(indexInfo.getNode()));
        }
        
        if (isDocumentRange(indexInfo)) {
            
            return createDocumentRanges(queryNode, shard, indexInfo, config.isTldQuery());
            
        } else if (isShardRange(shard)) {
            
            return createShardRange(queryNode, shard, indexInfo);
            
        } else {
            
            return createDayRange(queryNode, shard, indexInfo);
        }
    }
    
    /**
     * Building document ranges is only possible if the IndexInfo object contains document ids.
     *
     * @param indexInfo
     *            - object built from matches in the index.
     * @return - true if we can build document range(s).
     */
    public static boolean isDocumentRange(IndexInfo indexInfo) {
        return !indexInfo.uids().isEmpty();
    }
    
    /**
     *
     * @param shard
     * @return - true if the shard string is a shard range
     */
    public static boolean isShardRange(String shard) {
        return shard.indexOf('_') >= 0;
    }
    
    /**
     *
     *
     * @param queryNode
     * @param shard
     * @param indexMatches
     * @param isTldQuery
     * @return
     */
    public static Iterator<QueryPlan> createDocumentRanges(JexlNode queryNode, String shard, IndexInfo indexMatches, boolean isTldQuery) {
        List<QueryPlan> ranges = Lists.newArrayListWithCapacity(indexMatches.uids().size());
        
        for (IndexMatch indexMatch : indexMatches.uids()) {
            
            String docId = indexMatch.getUid();
            Range range;
            if (isTldQuery) {
                range = RangeFactory.createTldDocumentSpecificRange(shard, docId);
            } else {
                range = RangeFactory.createDocumentSpecificRange(shard, docId);
            }
            
            if (log.isTraceEnabled())
                log.trace(queryNode + " " + indexMatch.getNode());
            
            // don't really want log statement if uid.getNode is null
            
            // Log info if indexMatch is not null
            if (log.isTraceEnabled() && null != indexMatch.getNode()) {
                
                // query node can be null in this case
                log.trace("Building " + range + " from " + (null == queryNode ? "NoQueryNode" : JexlStringBuildingVisitor.buildQuery(queryNode)) + " actually "
                                + JexlStringBuildingVisitor.buildQuery(indexMatch.getNode()));
            }
            
            ranges.add(new QueryPlan(indexMatch.getNode(), range));
        }
        return ranges.iterator();
    }
    
    public static Iterator<QueryPlan> createShardRange(JexlNode queryNode, String shard, IndexInfo indexInfo) {
        JexlNode myNode = queryNode;
        if (indexInfo.getNode() != null) {
            myNode = indexInfo.getNode();
        }
        
        Range range = RangeFactory.createShardRange(shard);
        
        if (log.isTraceEnabled() && null != myNode) {
            log.trace("Building shard " + range + " From " + JexlStringBuildingVisitor.buildQuery(myNode));
        }
        
        return Collections.singleton(new QueryPlan(myNode, range)).iterator();
    }
    
    public static Iterator<QueryPlan> createDayRange(JexlNode queryNode, String shard, IndexInfo indexInfo) {
        JexlNode myNode = queryNode;
        if (indexInfo.getNode() != null) {
            myNode = indexInfo.getNode();
        }
        
        Range range = RangeFactory.createDayRange(shard);
        if (log.isTraceEnabled())
            log.trace("Building day" + range + " from " + (null == myNode ? "NoQueryNode" : JexlStringBuildingVisitor.buildQuery(myNode)));
        return Collections.singleton(new QueryPlan(myNode, range)).iterator();
    }
}
