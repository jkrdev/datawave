package datawave.query.predicate;

import com.google.common.base.Predicate;

import datawave.query.attributes.Document;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This filter will filter event data keys by only those fields that are required in the specified query.
 */
public interface EventDataQueryFilter extends Predicate<Map.Entry<Key,String>>, Filter, SeekingFilter, TransformingFilter, Cloneable {
    
    /**
     * This method is used to denote the start of processing a new document.
     *
     * @param documentKey
     */
    void startNewDocument(Key documentKey);
    
    /**
     * The apply method is used to determine what gets put into the resulting document and subsequently the jexl context for evaluation. Note that this must
     * also return those fields that will be eventually returned to the user. The keep method below will filter out that subset. The only caviat to this note is
     * that if this is being used with a query logic that completely throws away this document in order to return another (e.g.
     * ParentQueryIterator.mapDocument), then this method only needs to allow those fields required for evaluation.
     *
     * @see com.google.common.base.Predicate#apply(Object)
     * 
     * @param var1
     * @return true if keeping this field for the jexl context
     */
    @Override
    boolean apply(@Nullable Map.Entry<Key,String> var1);
    
    /**
     * The keep method is used to filter out those fields returned from the apply method above that will be returned to the user.
     * 
     * @see datawave.query.predicate.Filter#keep(org.apache.accumulo.core.data.Key)
     *
     * @return true if keeping this field
     */
    @Override
    boolean keep(Key k);
    
    /**
     * Define the start key given the from condition.
     *
     * @param from
     * @return
     */
    Key getStartKey(Key from);
    
    /**
     * Define the end key given the from condition.
     *
     * @param from
     * @return
     */
    Key getStopKey(Key from);
    
    /**
     * Get the key range that covers the complete document specified by the input key range
     *
     * @param from
     * @return
     */
    Range getKeyRange(Map.Entry<Key,Document> from);
    
    /**
     * Clone the underlying EventDataQueryFilter
     * 
     * @return
     */
    EventDataQueryFilter clone();
}
