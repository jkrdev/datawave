package datawave.accumulo.util.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import datawave.security.authorization.DatawaveUser;
import datawave.security.authorization.SubjectIssuerDNPair;
import org.apache.accumulo.core.security.Authorizations;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class UserAuthFunctionsTest {
    
    private static final UserAuthFunctions UAF = new UserAuthFunctions.Default();
    
    private static final String USER_DN = "userDN";
    private static final String ISSUER_DN = "issuerDN";
    private String requestedAuths;
    private HashSet<Set<String>> userAuths;
    
    private DatawaveUser user;
    private DatawaveUser p1;
    private DatawaveUser p2;
    private Collection<DatawaveUser> proxyChain;
    
    @Before
    public void initialize() {
        requestedAuths = "A,C";
        userAuths = new HashSet<>();
        userAuths.add(Sets.newHashSet("A", "C", "D"));
        userAuths.add(Sets.newHashSet("A", "B", "E"));
        
        SubjectIssuerDNPair userDN = SubjectIssuerDNPair.of(USER_DN, ISSUER_DN);
        SubjectIssuerDNPair p1dn = SubjectIssuerDNPair.of("entity1UserDN", "entity1IssuerDN");
        SubjectIssuerDNPair p2dn = SubjectIssuerDNPair.of("entity2UserDN", "entity2IssuerDN");
        
        user = new DatawaveUser(userDN, DatawaveUser.UserType.USER, Sets.newHashSet("A", "C", "D"), null, null, System.currentTimeMillis());
        p1 = new DatawaveUser(p1dn, DatawaveUser.UserType.SERVER, Sets.newHashSet("A", "B", "E"), null, null, System.currentTimeMillis());
        p2 = new DatawaveUser(p2dn, DatawaveUser.UserType.SERVER, Sets.newHashSet("A", "F", "G"), null, null, System.currentTimeMillis());
        proxyChain = Lists.newArrayList(user, p1, p2);
    }
    
    @Test
    public void testDowngradeAuthorizations() {
        HashSet<Authorizations> expected = Sets.newHashSet(new Authorizations("A", "C"), new Authorizations("A", "B", "E"), new Authorizations("A", "F", "G"));
        assertEquals(expected, UAF.mergeAuthorizations(UAF.getRequestedAuthorizations(requestedAuths, user), proxyChain, u -> u != user));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUserRequestsAuthTheyDontHave() {
        requestedAuths = "A,C,D,X,Y,Z";
        UAF.getRequestedAuthorizations(requestedAuths, user);
    }
    
    @Test
    public void testUserAuthsFirstInMergedSet() {
        HashSet<Authorizations> mergedAuths = UAF.mergeAuthorizations(UAF.getRequestedAuthorizations(requestedAuths, user), proxyChain, u -> u != user);
        assertEquals(3, mergedAuths.size());
        assertEquals("Merged user authorizations were not first in the return set", new Authorizations("A", "C"), mergedAuths.iterator().next());
    }
    
    @Test
    public void testUserIsNull() {
        assertEquals(Authorizations.EMPTY, UAF.getRequestedAuthorizations(requestedAuths, null));
    }
    
    @Test
    public void testRequestedAuthsIsNull() {
        assertEquals(new Authorizations("A", "C", "D"), UAF.getRequestedAuthorizations(null, user));
        assertEquals(new Authorizations("A", "C", "D"), UAF.getRequestedAuthorizations("", user));
    }
}
