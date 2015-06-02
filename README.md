# geoevent-datastore-proxy

This proxy is a J2EE Web Application that can be used in conjunction with GeoEvent Extension to ease the manageability of ArcGIS Data Store connections.  This is intended for use with GeoEvent Extension 10.3.0 or GeoEvent Processor 10.2.2 and earlier.


## Features
* Create proxied connections to ArcGIS Server or Portal where the proxy Web Application manages the tokens for the user.
* Place copies of the self-signed certificates inside of a directory so that the proxy automatically trusts those certificates.  With this feature you won't have to touch the JRE's cacerts file to get connections to work.
* Configure username and passwords for both the Web Tier and GIS Tier

## Requirements
To Build:

1. [Maven](http://maven.apache.org/download.cgi)
2. [Java JDK 7 or later](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

## Instructions

Building the source code:

1. Make sure [Maven](http://maven.apache.org/download.cgi) is installed on your machine.
2. Run 'mvn package'

Deploying built WAR file:

1. Using an instance of [Tomcat](http://tomcat.apache.org/), copy the file geoevent-datastore-proxy.war to TOMCAT_HOME\webapps
  2. You can use the instance of Tomcat that ships with ArcGIS Server when deploying this WAR file.
3. Run your instance of Tomcat.
  
## Configuration

###Servers

Once the webapplication has been unzipped, you will find a file TOMCAT_HOME\webapps\geoevent-datastore-proxy\WEB-INF\classes\arcgisservers.properties
This file holds the configurations for all of the proxied connections you want to make.

Below is a sample configuration
```
servers=javidel,portalhostqa,portaliwaqa
javidel.url=http://javidel.esri.com:6080/
javidel.tokenUrl=https://javidel.esri.com:6443/arcgis/tokens/generateToken
javidel.gisTierUsername=publisher
javidel.gisTierPassword=publisher
javidel.username=
javidel.password=
portalhostqa.url=https://portalhostqa.ags.esri.com/
portalhostqa.tokenUrl=https://portalhostqa.ags.esri.com/gis/sharing/rest/generateToken
portalhostqa.gisTierUsername=sharing1
portalhostqa.gisTierPassword=sharing.1
portalhostqa.username=
portalhostqa.password=
portaliwaqa.url=https://portaliwaqa.ags.esri.com/
portaliwaqa.tokenUrl=https://portaliwaqa.ags.esri.com/gis/sharing/rest/generateToken
portaliwaqa.gisTierUsername=
portaliwaqa.gisTierPassword=
portaliwaqa.username=
portaliwaqa.password=
```

This configuration configure the proxy for 3 servers.

1. javidel: ArcGIS Server that uses tokens to secure servers.
2. portlhostqa: A portal instance that uses tokens to secure services
3. portaliwaqa: A portal instance that uses IWA to protect its services.
 
The main entry for the configuration is the `servers` property.  This property is a Comma Separated Values (CSV) list of server names that you want to configure.  Each element in the CSV list will server as a key to other properties within the properties file.  For each serverName, you can configure the following properties:

* &lt;serverName&gt;.url **REQUIRED**
```
The base URL that you want to proxy.  Usually http://myhost.com/
```
* &lt;serverName&gt;.tokenUrl
```
The URL where the proxy should POST a request to generate a token.  This must end in with `generateToken` 
otherwise the POST operation will most likely fail and the proxy will not send any tokens with requests.
If not present, then the proxy will not append or replace any tokens associated with proxied requests.
```
* &lt;serverName&gt;.username
```
The username to present to server when WebTier authentication is requested.
```
* &lt;serverName&gt;.password
```
The password to present to server when WebTier authentication is requested.
```
* &lt;serverName&gt;.gisTierUsername
```
The username to pass to the generateToken request.  If you're configuring a Portal or Web Adaptor that uses
Integrated Windows Authentication (IWA) and the Tomcat process where the proxy is deployed runs as a user 
with apopr
```
* &lt;serverName&gt;.gisTierPassword
```
The password to pass to the generateToken request.
```

####Configuration Use Cases
##### javidel (Server using GIS Tier authentication with tokens)
The configuration for javidel is for an ArcGIS Server that uses tokens to protect some of its services.  It is a stand-alone You visit http://javidel.esri.com:6080/arcgis/rest/info?f=pjson to retrieve the server's information and see the following:
```
{
 "currentVersion": 10.4,
 "fullVersion": "10.4.0",
 "soapUrl": "http://javidel.esri.com:6080/arcgis/services",
 "secureSoapUrl": "https://javidel.esri.com:6443/arcgis/services",
 "authInfo": {
  "isTokenBasedSecurity": true,
  "tokenServicesUrl": "https://javidel.esri.com:6443/arcgis/tokens/",
  "shortLivedTokenValidity": 1
 }
}
```
From the JSON, we see that the token url is at https://javidel.esri.com:6443/arcgis/tokens/, but we need to endpoint where we would POST the token request, so we add generateToken to the path.  So the property javidel.tokenURL=https://javidel.esri.com:6443/arcgis/tokens/generateToken gets set.

Since the javidel server does not use WebTier authentiation, we leave the javidel.username and javidel.password entries blank.  Since the tokens are needed for GISTier authenctication, we supply the javidel.gisTierUsername and javidel.gisTierPassword properties.

#####portalhostqa (Portal with GIS Tier authentication using tokens)
The configuration for portalhostqa is for an ArcGIS Portal Server that uses tokens to protect its services.  You visit https://portalhostqa.ags.esri.com/gis/sharing/rest/info?f=pjson to retrieve the server's information and see the following:
```
{
  "owningSystemUrl": "https://portalhostqa.ags.esri.com/gis",
  "authInfo": {
    "tokenServicesUrl": "https://portalhostqa.ags.esri.com/gis/sharing/rest/generateToken",
    "isTokenBasedSecurity": true
  }
}
```
From the JSON, we see that the token url is at https://portalhostqa.ags.esri.com/gis/sharing/rest/generateToken. So the property portalhostqa.tokenUrl=https://portalhostqa.ags.esri.com/gis/sharing/rest/generateToken gets set.

Since the portalhostqa server does not use WebTier authentiation, we leave the portalhostqa.username and portalhostqa.password entries blank.  Since the tokens are needed for GISTier authenctication, we supply the portalhostqal.gisTierUsername and portalhostqa.gisTierPassword properties.

##### portaliwaqa (Portal using WebTier authentication with IWA)
The configuration for portaliwaqa is for an ArcGIS Portal Server that uses Integrated Windows Authentication (IWA) to protect its services.  It is a stand-alone You visit https://portaliwaqa.ags.esri.com/gis/sharing/rest/info?f=pjson to retrieve the server's information and see the following:
```
{
  "owningSystemUrl": "https://portaliwaqa.ags.esri.com/gis",
  "authInfo": {
    "tokenServicesUrl": "https://portaliwaqa.ags.esri.com/gis/sharing/rest/generateToken",
    "isTokenBasedSecurity": true
  }
}
```
From the JSON, we see that the token url is at https://portaliwaqa.ags.esri.com/gis/sharing/rest/generateToken. So the property portaliwaqa.tokenUrl=https://portaliwaqa.ags.esri.com/gis/sharing/rest/generateToken gets set.

For this to work, the Tomcat process must be run as a user that has enough access privileges to consume the services since we did not provide username password for neither the WebTier nor the GIS Tier.  The proxy will impersonate the user that is running the Web Application process when communicating with the server in this case.  If the user that is running the process cannot consume the services, then this configuration would need the portaliwaqa.gisTierUsername and portaliwaqa.gisTierPassword properties set to consume the services. 

####Certificates
If any of the sites you're connecting to use self-signed Https certificates, then you can export them using the browser and place them in the directory &lt;TOMCAT&gt;/webapps/geoevent-datastore-proxy/WEB-INF/classes/certificates with the ending of .crt, .cert, or .pem.  And certificate listed in the directory will be trusted by the proxy when making connections.

###Restarting the proxy
After making modifications to either the arcgisservers.properties file or dropping a new certificate file into the certificates directory will require a restart of the Tomcat server.

###Configuring GeoEvent DataStores
Once you've got the proxy configured, you can point GeoEvent DataStores to use the proxy's endpoints instead of going directly to the ArcGIS Server or Portal.  The screen shots for the rest of this section assume the Tomcat proxy is running at http://localhost:8080/geoevent-datastore-proxy/ (where localhost would be the same machine where GeoEvent is running).

####javidel
Here are the configurations for javidel:

![App](javidel.png?raw=true)

####portalhostqa
Here are the configurations for portalhostqa:

![App](portalhostqa.png?raw=true)

*NOTE: when configuring with GeoEvent 10.2.2 and earlier, you'll need to enter some value for the token when configuring a Portal connection.  You can enter any string here when using the Proxy since the Proxy will replace the token*

####portaliwaqa
Here are the configurations for portaliwaqa:

![App](portaliwaqa.png?raw=true)

*NOTE: when configuring with GeoEvent 10.2.2 and earlier, you'll need to enter some value for the token when configuring a Portal connection.  You can enter any string here when using the Proxy since the Proxy will replace the token*