package org.nuxeo.ecm.restapi.server;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.server.jaxrs.OnlyOfficeResource;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, PlatformFeature.class })
@Deploy({ "nuxeo.onlyoffice.rest" })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OnlyOfficeResourceTest extends BaseTest {

  @Inject
  CoreSession session;

  private WebResource resource;

  @Test
  public void shouldRedirectToOnlyOffice() throws Exception {
    RuntimeService runtime = Framework.getRuntime();
    runtime.setProperty(OnlyOfficeResource.URL_API, "http://localhost/api.js");
    runtime.setProperty(OnlyOfficeResource.PRELOADER, "http://localhost/preloader");

    resource = getServiceFor(REST_API_URL, "Administrator", "Administrator");

    ClientResponse response = resource.path("onlyoffice/api/documents/api.js").get(ClientResponse.class);
    Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    response = resource.path("onlyoffice/api/documents/cache-scripts.html").get(ClientResponse.class);
    Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldConsumeCallback() throws Exception {
    //
    // resource = getServiceFor(REST_API_URL, "Administrator", "Administrator");
    //
    // final String obXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><form
    // xmlns:fr=\"http://orbeon.org/oxf/xml/form-runner\" fr:data-format-version=\"4.0.0\">\n"
    // + " <dublincore-section>\n" + " <title>test</title>\n"
    // + " <description>tets</description>\n"
    // + " </dublincore-section>\n" + " <Note-section>\n"
    // + " <note>etetet</note>\n" + " </Note-section>\n" + "</form>";
    //
    // ClientResponse response =
    // resource.path("orbeon/crud/Nuxeo/SimpleForm2/data/new-id/data.xml").entity(obXML).put(
    // ClientResponse.class);
    //
    // assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    // session.save();
    //
    // response = resource.path("orbeon/search/Nuxeo/SimpleForm2").entity("").post(ClientResponse.class);
    //
    // assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    // String xml = response.getEntity(String.class);
    //
    // System.out.print(xml);

  }

}
