package com.example.sonos

import scala.xml.{MetaData, NamespaceBinding, Elem}

/**
 * Case class for constructing the SOAP calls to the SONOS api.
 * == Message format ==
 *
 * The message format for the SOAP calls should look something like this.
 * If the SOAP action takes no parameters, the `<s:Body>` element can be empty.
 *
 * Service types are shown in the `device_description.xml` response.
 *  {{{
 * <?xml version="1.0"?>
 *	<s:Envelope
 *		xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
 *		s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
 *		<s:Body>
 *			<u:actionName
 *				xmlns:u="urn:schemas-upnp-org:service:serviceType:v">
 *			</u:actionName>
 *		</s:Body>
 *	</s:Envelope>
 *  }}}
 *
 * @param serviceType the service type. (e.g. `ZoneGroupTopology`)
 * @param version the service version. (e.g. 1)
 * @param action as described in the scpd. (e.g. `/xml/ZoneGroupTopology1.xml`)
 * @param arguments any arguments for the SOAP action.
 */
case class SonosCommand(serviceType:String, version:Int, action:String, arguments:Map[String,String]) {
	/**
	 * SOAPACTION header for the http POST call to the SOAP endpoint.
	 */
	val actionHeader = s"SOAPACTION:$serviceTypeNamespace:$version#$action"

	/**
	 * xml namespace for the SOAP call
	 */
	lazy val serviceTypeNamespace = s"urn:schemas-upnp-org:service:serviceType:$serviceType"


	/**
	 * XML template for the SOAP (ugh) message.
	 */
	private val envelope = <s:Envelope
	xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
	s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
		<s:Body>
		</s:Body>
	</s:Envelope>

	/**
	 * Return the SOAP message for the command
	 * @return XML SOAP message
	 */
	def soapXml:Elem = {
		val children = arguments.foldRight(List.empty[Elem])((p,acc) => Elem(null,p._1,xml.Null,xml.TopScope,minimizeEmpty = true, xml.Text(p._2)) +: acc)
		Elem("u",action, xml.Null ,NamespaceBinding("u",serviceTypeNamespace, null), minimizeEmpty = true, children:_*)
	}


}
