package org.nuxeo.ecm.onlyoffice.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class OnlyOfficeConverter implements ExternalConverter {

  private static final Logger LOG = LoggerFactory.getLogger(OnlyOfficeConverter.class);

  /**
   * OnlyOffice conversion URL
   */
  public static final String CONV_URL = "onlyoffice.url.conversion";

  /**
   * OnlyOffice conversion wait
   */
  public static final String CONV_WAIT = "onlyoffice.conversion.wait";

  public static final String CONV_PARAM_ASYNC = "async";

  public static final String CONV_PARAM_SRC_TYPE = "srcType";

  public static final String CONV_PARAM_DEST_TYPE = "destType";

  public static final String CONV_PARAM_CODE_PAGE = "codePage";

  public static final String CONV_PARAM_DELIMITER = "delimiter";

  public static final String CONV_PARAM_THUMBNAIL = "thumbnail";

  private ConverterDescriptor descriptor = null;

  private MimetypeRegistry mimeTypeRegistry = null;

  private DownloadService downloadService = null;

  private Client client = null;

  private String endpoint = null;

  private long waitTime = 1000L;

  private ObjectWriter requestWriter = null;

  private ObjectReader responseReader = null;

  private List<ConversionCompatibility> compat = null;

  public OnlyOfficeConverter() {
    super();
  }

  @Override
  public void init(ConverterDescriptor descriptor) {
    this.mimeTypeRegistry = Framework.getService(MimetypeRegistry.class);
    this.downloadService = Framework.getService(DownloadService.class);

    this.descriptor = descriptor;
    this.endpoint = Framework.getProperty(CONV_URL);
    // XXX: number type
    this.waitTime = Long.parseLong(Framework.getProperty(CONV_WAIT, "1000"));

    ObjectMapper mapper = new ObjectMapper();
    this.requestWriter = mapper.writerFor(ConversionRequest.class);
    this.responseReader = mapper.readerFor(ConversionResponse.class);

    try (InputStream in = getClass().getResourceAsStream("/reference/conversion_matrix.json")) {
      this.compat = mapper.readValue(in, new TypeReference<List<ConversionCompatibility>>() {
      });
    } catch (IOException iox) {

    }
  }

  private String getParam(Map<String, Serializable> parameters, String key, String defVal) {
    if (parameters.containsKey(key)) {
      return String.valueOf(parameters.get(key));
    }
    return defVal;
  }

  @Override
  public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
    Blob originalBlob = blobHolder.getBlob();
    String path = blobHolder.getFilePath();

    String srcType = findType(originalBlob.getMimeType(), originalBlob);
    String destType = findType(this.descriptor.getDestinationMimeType(), null);

    // Check compatibility
    boolean compatConversion = false;
    for (ConversionCompatibility cc : this.compat) {
      if (cc.accepts(srcType, destType)) {
        compatConversion = true;
        break;
      }
    }
    if (!compatConversion) {
      String errMsg = String.format("Incompatible conversion: %s -> %s for blob %s", srcType, destType,
          originalBlob.getFilename());
      LOG.error(errMsg);
      throw new ConversionException(errMsg);
    }

    TokenAuthenticationService tokens = Framework.getService(TokenAuthenticationService.class);
    String token = null;

    Blob conversion;
    try {
      // Prepare blob
      String storeKey = this.downloadService.storeBlobs(Collections.singletonList(originalBlob));
      token = tokens.acquireToken("Administrator", "ONLYOFFICE", storeKey, "ONLYOFFICE Conversion Service", "rw");

      String nuxeoUrl = Framework.getProperty("nuxeo.url");
      String url = nuxeoUrl + "/" + this.downloadService.getDownloadUrl(storeKey) + "?token=" + token;

      // Create request
      ConversionRequest request = new ConversionRequest();
      request.setAsync("true".equals(getParam(parameters, CONV_PARAM_ASYNC, "true")));
      request.setKey(storeKey);

      request.setFileType(srcType);

      request.setOutputType(destType);
      request.setTitle(newFilename(originalBlob.getFilename(), destType));
      request.setUrl(url);

      if ("csv".equals(srcType)) {
        handleCodePage(request, parameters);
        handleDelimeter(request, parameters);
      } else if ("txt".equals(srcType)) {
        handleCodePage(request, parameters);
      }

      if ("bmp".equals(destType) || "gif".equals(destType) || "jpg".equals(destType) || "png".equals(destType)) {
        if (parameters.containsKey("thumbnail")) {
          handleThumbnail(request, parameters);
        }
      }

      LOG.warn("{}", request);

      conversion = convert(request);
    } catch (IOException | InterruptedException | ConversionException e) {
      throw new ConversionException("Cannot convert " + path + " to " + this.descriptor.getDestinationMimeType(), e);
    } finally {
      tokens.revokeToken(token);
    }
    return new SimpleBlobHolder(conversion);
  }

  private void handleDelimeter(ConversionRequest request, Map<String, Serializable> parameters) {
    Serializable value = parameters.get(CONV_PARAM_DELIMITER);
    int delimiter = ConversionConstants.DELIMITER_COMMA;
    if (value != null) {
      if (";".equals(value)) {
        delimiter = ConversionConstants.DELIMITER_SEMICOLON;
      } else if (":".equals(value)) {
        delimiter = ConversionConstants.DELIMITER_COLON;
      } else {
        try {
          delimiter = Integer.parseInt(value.toString());
          if (delimiter < 0 || delimiter > 5) {
            delimiter = ConversionConstants.DELIMITER_COMMA;
            LOG.warn("Delimiter setting is invalid, ignoring: " + value);
          }
        } catch (NumberFormatException nfe) {
          LOG.warn("Delimiter setting is invalid, ignoring: " + value, nfe);
        }
      }
    }
    request.setDelimiter(delimiter);
  }

  private void handleCodePage(ConversionRequest request, Map<String, Serializable> parameters) {
    Serializable value = parameters.get(CONV_PARAM_CODE_PAGE);
    int code = ConversionConstants.CODE_UNICODE;
    if (value != null) {
      // TODO: Check reference table (?)
      try {
        code = Integer.parseInt(value.toString());
      } catch (NumberFormatException nfe) {
        LOG.warn("Code page setting is invalid, ignoring: " + value, nfe);
      }
    }
    request.setCodePage(code);
  }

  private void handleThumbnail(ConversionRequest request, Map<String, Serializable> parameters) {
    Serializable value = parameters.get(CONV_PARAM_THUMBNAIL);
    if (!"false".equals(value)) {
      return;
    }
    ConversionRequest.Thumbnail thumb = request.createThumbnail();
    if (value != null) {
      if ("true".equals(value)) {
        return;
      }
      String[] size = value.toString().split(":");
      try {
        if (size.length > 0) {
          thumb.setHeight(Integer.parseInt(size[0]));
        }
        if (size.length > 1) {
          thumb.setWidth(Integer.parseInt(size[1]));
        }
        if (size.length > 2) {
          thumb.setAspect(Integer.parseInt(size[2]));
        }
      } catch (NumberFormatException nfe) {
        LOG.warn("Height/Width/Aspect setting for thumbnail is not specific, ignoring: " + value, nfe);
      }
    }
  }

  private String findType(String mimeType, Blob blob) {
    if (mimeType == null && blob != null) {
      mimeType = blob.getMimeType();
      if (mimeType == null) {
        // Get from file
        mimeType = this.mimeTypeRegistry.getMimetypeFromBlob(blob);
      }
    }

    if (mimeType != null) {
      List<String> types = this.mimeTypeRegistry.getExtensionsFromMimetypeName(mimeType);
      if (!types.isEmpty()) {
        return types.get(0);
      }
    }

    throw new ConversionException("Unable to locate extension for type: " + mimeType);
  }

  private String newFilename(String original, String ext) {
    if (original == null) {
      return UUID.randomUUID().toString() + "." + ext;
    }
    int idx = original.lastIndexOf('.');
    if (idx > 0) {
      return original.substring(0, idx) + ext;
    } else {
      return original + "." + ext;
    }
  }

  @Override
  public ConverterCheckResult isConverterAvailable() {
    if (this.endpoint == null || "".equals(this.endpoint.trim())) {
      String err = "Please configure `onlyoffice.url.conversion` to enable conversion service.";
      LOG.warn("ONLYOFFICE conversion disabled: " + err);
      return new ConverterCheckResult("ONLYOFFICE installation required", err);
    }
    return new ConverterCheckResult();
  }

  private Blob convert(ConversionRequest request) throws IOException, InterruptedException {
    // Request string remains the same for the life of the conversion
    String json = this.requestWriter.writeValueAsString(request);

    boolean running = true;
    ConversionResponse response = null;

    // Process request
    while (running) {
      // Ask for conversion / ask for update
      response = submitRequest(json);

      // Break loop if complete/error
      if (response.isFinished()) {
        running = false;
        break;
      }

      // Wait a bit...
      LOG.warn("Waiting for {}", request);
      Thread.sleep(this.waitTime);
    }

    // Handle response
    LOG.warn("{}", response);
    if (response.isError()) {
      // Report error and exit
      return null;
    } else {
      // Retrieve content
      return retrieveResponse(response);
    }
  }

  private Client client() {
    if (this.client == null) {
      ClientConfig cc = new DefaultClientConfig();
      cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
      this.client = Client.create(cc);
    }
    return this.client;
  }

  private WebResource resource(final String endpoint) {
    return client().resource(endpoint);
  }

  private ConversionResponse submitRequest(String request) throws IOException {
    WebResource.Builder builder = resource(this.endpoint).accept(MediaType.APPLICATION_JSON);
    builder = builder.entity(request, MediaType.APPLICATION_JSON);
    String response = builder.post(String.class);
    return this.responseReader.readValue(response);
  }

  private Blob retrieveResponse(ConversionResponse response) throws IOException {
    WebResource resource = resource(response.getFileUrl());
    InputStream data = resource.get(InputStream.class);
    Blob blob = Blobs.createBlob(data);
    return blob;
  }

}
