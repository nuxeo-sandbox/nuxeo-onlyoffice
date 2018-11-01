package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * This web object implements the Callback functionality specified by the OnlyOffice API. See:
 * https://api.onlyoffice.com/editors/callback
 */
@WebObject(type = "onlyoffice")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public class OnlyOfficeResource extends DefaultObject {

    protected static final Logger LOG = LoggerFactory.getLogger(OnlyOfficeResource.class);

    /**
     * OnlyOffice api.js URL
     */
    public static final String URL_API = "onlyoffice.url.api";

    /**
     * Create a new document version on save callback.
     */
    public static final String VERSION_ON_SAVE = "onlyoffice.version.save";

    protected boolean versionOnSave = false;

    private static final ObjectReader CALLBACK;

    private static final Client CLIENT;

    private static final AtomicBoolean CONNECTED = new AtomicBoolean(false);

    static {
        ObjectMapper mapper = new ObjectMapper();
        CALLBACK = mapper.readerFor(OnlyOfficeCallback.class);

        ClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
        CLIENT = Client.create(cc);
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.model.impl.AbstractResource#initialize(java.lang.Object[])
     */
    @Override
    protected void initialize(Object... args) {

        this.versionOnSave = "true".equalsIgnoreCase(Framework.getProperty(VERSION_ON_SAVE, "false"));

        // Obtain API URL
        String apiUrl = Framework.getProperty(URL_API);
        if (apiUrl == null || "".equals(apiUrl.trim())) {
            LOG.warn("ONLYOFFICE api.js URL has not been set, please set `onlyoffice.url.api` in nuxeo.conf."
                    + "\n  onlyoffice.url.api=http://onlyoffice.host/web-apps/apps/api/documents/api.js");
        }

        // Test client
        if (!CONNECTED.get()) {
            CONNECTED.set(testAPIConnection(apiUrl));
        }
    }

    private boolean testAPIConnection(String url) {
        WebResource resource = CLIENT.resource(url);
        WebResource.Builder builder = resource.accept("application/javascript");
        boolean success = false;
        try {
            ClientResponse response = builder.get(ClientResponse.class);
            if (response.getStatus() < 200 || response.getStatus() >= 400) {
                LOG.warn("Unable to reach ONLYOFFICE API: {}", response.getStatusInfo());
            } else {
                LOG.info("Connected to ONLYOFFICE Editor [{}] ({})", url, response.getStatus());
                success = true;
            }
        } catch (Exception ex) {
            LOG.warn("Error connecting to ONLYOFFICE API", ex);
        }
        return success;
    }

    /**
     * Callback endpoint for OnlyOffice
     *
     * @param id document ID
     * @param xpath blob path
     * @param input input stream JSON
     * @return save status
     * @throws Exception
     */
    @POST
    @Path("callback/{id}/{xpath}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCallback(@PathParam("id") String id, @PathParam("xpath") String xpath, InputStream input) {

        /**
         * (from https://api.onlyoffice.com/editors/callback) Defines the status of the document. Can have the following
         * values: 0 - no document with the key identifier could be found, 1 - document is being edited, 2 - document is
         * ready for saving, 3 - document saving error has occurred, 4 - document is closed with no changes, 6 -
         * document is being edited, but the current document state is saved, 7 - error has occurred while force saving
         * the document.
         */
        try {
            String json = IOUtils.toString(input, Charset.defaultCharset());
            OnlyOfficeCallback callback = CALLBACK.readValue(json);

            if (LOG.isDebugEnabled()) {
                LOG.debug("JSON: {}", json);
                LOG.debug(callback.toString());
            }

            LOG.warn(callback.toString());

            int status = callback.getStatus();
            if (status >= 1 && status <= 6) {
                CoreSession session = getContext().getCoreSession();
                DocumentModel model = session.getDocument(new IdRef(id));

                // Record editors for current document
                List<String> editors = callback.getUsers();
                if (!model.hasFacet("onlyoffice")) {
                    model.addFacet("onlyoffice");
                }
                model.setProperty("onlyoffice", "editors", editors);

                if (callback.isModified() && callback.getUrl() != null) {
                    BlobHolder blobHolder = model.getAdapter(BlobHolder.class);
                    Blob original = blobHolder.getBlob();

                    WebResource resource = CLIENT.resource(callback.getUrl());
                    WebResource.Builder builder = resource.accept(MediaType.WILDCARD);

                    Blob saved = null;
                    ClientResponse response = builder.get(ClientResponse.class);
                    try (InputStream stream = response.getEntityInputStream()) {
                        saved = Blobs.createBlob(stream, original.getMimeType(), original.getEncoding());
                        saved.setFilename(original.getFilename());
                    } finally {
                        response.close();
                    }

                    blobHolder.setBlob(saved);

                    // Status is 2 or 3
                    if (status < 4 && this.versionOnSave) {
                        saveVersion(model, callback.getStatus() == 2);
                    }
                }

                // Remove lock obtained on edit request
                // Don't unlock on status 6
                if (status >= 2 && status < 6 && model.isLocked()) {
                    model.removeLock();
                }

                session.saveDocument(model);
                session.save();
            }
        } catch (IOException iox) {
            LOG.error("Error saving ONLYOFFICE callback", iox);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"error\":1}").build();
        }

        return Response.status(Status.OK).entity("{\"error\":0}").build();
    }

    private DocumentModel saveVersion(DocumentModel doc, boolean major) {
        if (!doc.hasFacet(FacetNames.VERSIONABLE)) {
            LOG.warn("Unable to save version for OnlyOffice document: '{}'", doc.getId());
            return doc;
        }

        VersioningOption vo = major ? VersioningOption.MAJOR : VersioningOption.MINOR;
        doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
        return doc;
    }

}
