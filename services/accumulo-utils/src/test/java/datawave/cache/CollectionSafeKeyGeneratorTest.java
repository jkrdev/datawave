package datawave.cache;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.cache.interceptor.SimpleKey;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Test the CollectionSafeKeyGenerator class
 */
public class CollectionSafeKeyGeneratorTest {
    
    @Test
    public void testListCopy() {
        ArrayList<Object> list = new ArrayList<>();
        list.add(new Object());
        Object copy = CollectionSafeKeyGenerator.copyIfCollectionParam(list);
        Assert.assertNotSame(copy, list);
        Assert.assertEquals(copy, list);
    }
    
    @Test
    public void testSetCopy() {
        HashSet<Object> set = new HashSet<>();
        set.add(new Object());
        Object copy = CollectionSafeKeyGenerator.copyIfCollectionParam(set);
        Assert.assertNotSame(copy, set);
        Assert.assertEquals(copy, set);
    }
    
    @Test
    public void testSortedSetCopy() {
        TreeSet<String> set = new TreeSet<>();
        set.add("Hello World");
        Object copy = CollectionSafeKeyGenerator.copyIfCollectionParam(set);
        Assert.assertNotSame(copy, set);
        Assert.assertEquals(copy, set);
    }
    
    @Test
    public void testMapCopy() {
        HashMap<Object,Object> map = new HashMap<>();
        map.put(new Object(), new Object());
        Object copy = CollectionSafeKeyGenerator.copyIfCollectionParam(map);
        Assert.assertNotSame(copy, map);
        Assert.assertEquals(copy, map);
    }
    
    @Test
    public void testSortedMapCopy() {
        TreeMap<String,Object> map = new TreeMap<>();
        map.put("Hello World", new Object());
        Object copy = CollectionSafeKeyGenerator.copyIfCollectionParam(map);
        Assert.assertNotSame(copy, map);
        Assert.assertEquals(copy, map);
    }
    
    @Test
    public void testEmptyKey() {
        Object key = CollectionSafeKeyGenerator.generateKey();
        Assert.assertSame(key, SimpleKey.EMPTY);
    }
    
    @Test
    public void testNonCollectionSingleParam() {
        Object obj1 = new Object();
        Object key = CollectionSafeKeyGenerator.generateKey(obj1);
        Assert.assertSame(key, obj1);
    }
    
    @Test
    public void testNonCollectionMultiParam() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object key = CollectionSafeKeyGenerator.generateKey(obj1, obj2);
        Assert.assertTrue(key instanceof SimpleKey);
        Assert.assertEquals(new SimpleKey(obj1, obj2), key);
    }
    
    @Test
    public void testCollectionSingleParam() {
        ArrayList<Object> list = new ArrayList<>();
        list.add(new Object());
        Object key = CollectionSafeKeyGenerator.generateKey(list);
        Assert.assertNotSame(key, list);
        Assert.assertEquals(key, list);
    }
    
    @Test
    public void testCollectionMultiParam() throws IllegalAccessException, NoSuchFieldException {
        ArrayList<Object> list = new ArrayList<>();
        list.add(new Object());
        HashSet<Object> set = new HashSet<>();
        set.add(new Object());
        Object key = CollectionSafeKeyGenerator.generateKey(list, set);
        Assert.assertTrue(key instanceof SimpleKey);
        Assert.assertEquals(new SimpleKey(list, set), key);
        // extract the SimpleKey params and test those
        Field field = SimpleKey.class.getDeclaredField("params");
        field.setAccessible(true);
        Object[] params = (Object[]) field.get(key);
        Assert.assertEquals(2, params.length);
        Assert.assertNotSame(params[0], list);
        Assert.assertEquals(params[0], list);
        Assert.assertNotSame(params[1], set);
        Assert.assertEquals(params[1], set);
    }
    
    @Test
    public void testMixedMultiParam() throws IllegalAccessException, NoSuchFieldException {
        ArrayList<Object> list = new ArrayList<>();
        list.add(new Object());
        Object obj1 = new Object();
        HashSet<Object> set = new HashSet<>();
        set.add(new Object());
        Object obj2 = new Object();
        Object key = CollectionSafeKeyGenerator.generateKey(list, obj1, set, obj2);
        Assert.assertTrue(key instanceof SimpleKey);
        Assert.assertEquals(new SimpleKey(list, obj1, set, obj2), key);
        // extract the SimpleKey params and test those
        Field field = SimpleKey.class.getDeclaredField("params");
        field.setAccessible(true);
        Object[] params = (Object[]) field.get(key);
        Assert.assertEquals(4, params.length);
        Assert.assertNotSame(params[0], list);
        Assert.assertEquals(params[0], list);
        Assert.assertSame(params[1], obj1);
        Assert.assertNotSame(params[2], set);
        Assert.assertEquals(params[2], set);
        Assert.assertSame(params[3], obj2);
    }
    
}
