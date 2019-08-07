package datawave.webservice.dictionary;

import datawave.webservice.edgedictionary.RemoteEdgeDictionary;
import org.apache.http.client.utils.URIBuilder;
import org.xbill.DNS.TextParseException;

import javax.annotation.security.PermitAll;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URISyntaxException;

/**
 * A simple proxy that redirects GET requests for the EdgeDictionary to the external dictionary service that is configured in the {@link RemoteEdgeDictionary}.
 * This allows existing documentation URLs to continue to work.
 */
@Path("/EdgeDictionary")
@LocalBean
@Stateless
@PermitAll
public class EdgeDictionaryBean {
    
    @Inject
    private RemoteEdgeDictionary remoteEdgeDictionary;
    
    /**
     * The edge dictionary only has one endpoint. Send redirects for any request to it.
     */
    @GET
    @Path("/")
    public Response getEdgeDictionary(@Context UriInfo uriInfo) throws TextParseException, URISyntaxException {
        URIBuilder builder = remoteEdgeDictionary.buildURI("");
        uriInfo.getQueryParameters().forEach((pname, valueList) -> valueList.forEach(pvalue -> builder.addParameter(pname, pvalue)));
        return Response.temporaryRedirect(builder.build()).build();
    }
}
