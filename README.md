# Nuxeo ONLYOFFICE Integration

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-onlyoffice-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-onlyoffice-master/)

In-browser integration of Nuxeo Platform and [ONLYOFFICE](https://www.onlyoffice.com/).

## Dependencies

[ONLYOFFICE](https://www.onlyoffice.com/) Document Server (Run a [Docker](https://github.com/ONLYOFFICE/Docker-DocumentServer) image)

## Build and Install

Build with maven (at least 3.3)

```
mvn clean install
```
> Package built here: `nuxeo-onlyoffice-package/target`

> Install with `nuxeoctl mp-install <package>`

## Configure (nuxeo.conf)

Editor properties:

```
# URL to editor api.js service (required)
onlyoffice.url.api=http://onlyoffice/web-apps/apps/api/documents/api.js
# Create version on save (optional, default: false)
onlyoffice.version.save=true|false
```

Conversion properties (Optional):

```
# URL to conversion service (see ONLYOFFICE docs)
onlyoffice.url.conversion=http://onlyoffice/ConvertService.ashx
# Number of millisecond to wait between polling async request
onlyoffice.conversion.wait=1000
```

> (!) When using Docker, use an accessible hostname for the IP URLs.

## (Optional) Use Conversion Service

Invoke the conversion service to transform between a variety of content types.  By default, the [office2pdf contribution](/nuxeo-onlyoffice-core/src/main/resources/OSGI-INF/onlyoffice-conversion-contrib.xml) will support PDF as a destination type.  See the [ONLYOFFICE Conversion API](https://api.onlyoffice.com/editors/conversionapi) for a full conversion matrix.

### Conversion Parameters

|Key      |Description|Allowable Values|
|---------|-----------|----------------|
|async    |Async Processing|true, false     |
|srcType  |Source Type|allowable type  |
|destType |Dest Type|allowable type  |
|codePage |Code Page|code page ref   |
|delimiter|Delimiter|delimiter ref   |
|thumbnail|Thumbnail|true, false, (height):(width):(aspect)|

### Rendition example

```xml
  <extension target="org.nuxeo.ecm.platform.rendition.service.RenditionService" point="renditionDefinitions">
    <renditionDefinition name="onlyoffice">
      <label>label.rendition.onlyofficepdf</label>
      <icon>/icons/note.gif</icon>
      <contentType>application/pdf</contentType>
      <operationChain>onlyofficePdf</operationChain>
      <storeByDefault>false</storeByDefault>
    </renditionDefinition>
  </extension>

  <extension target="org.nuxeo.ecm.core.operation.OperationServiceComponent" point="chains">
    <chain id="onlyofficePdf">
      <operation id="Context.PopBlob"/>
      <operation id="Blob.RunConverter">
        <param name="converter" type="string">office2pdf</param>
        <param name="parameters" type="properties">async=false</param>
      </operation>
    </chain>
  </extension>
```

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).

