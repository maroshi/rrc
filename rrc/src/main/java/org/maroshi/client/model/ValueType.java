/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Russell Boykin       - initial API and implementation
 *     Alberto Giammaria    - initial API and implementation
 *     Chris Peters         - initial API and implementation
 *     Gianluca Bernardini  - initial API and implementation
 *******************************************************************************/
package org.maroshi.client.model;

import java.net.URI;

import org.eclipse.lyo.oslc4j.core.model.OslcConstants;

public enum ValueType {
    Boolean(OslcConstants.XML_NAMESPACE + "boolean"),
	DateTime(OslcConstants.XML_NAMESPACE + "dateTime"),
	Time(OslcConstants.XML_NAMESPACE + "date"),
	Duration(OslcConstants.XML_NAMESPACE + "duration"),
	Date(OslcConstants.XML_NAMESPACE + "time"),
	Decimal(OslcConstants.XML_NAMESPACE + "decimal"),
	Double(OslcConstants.XML_NAMESPACE + "double"),
	Float(OslcConstants.XML_NAMESPACE + "float"),
	Integer(OslcConstants.XML_NAMESPACE + "integer"),
	Int(OslcConstants.XML_NAMESPACE + "int"),
	String(OslcConstants.XML_NAMESPACE + "string"),
	XMLLiteral(OslcConstants.RDF_NAMESPACE + "XMLLiteral"),
	Resource(OslcConstants.OSLC_CORE_NAMESPACE + "Resource"),
	User(OslcConstants.OSLC_CORE_NAMESPACE + "Resource"),
	LocalResource("http://www.ibm.com/xmlns/rdm/types/UserAttributeType"),
    AnyResource(OslcConstants.OSLC_CORE_NAMESPACE + "AnyResource"); // AnyResource not supported by OSLC4J

	private final String uri;

	ValueType(final String uri) {
		this.uri = uri;
	}

	@Override
    public String toString() {
		return uri;
	}

	public static ValueType fromString(final String string) {
        final ValueType[] values = ValueType.values();
        for (final ValueType value : values) {
            if (value.uri.equals(string)) {
                return value;
            }
        }
        return null;
    }

	public static ValueType fromURI(final URI uri) {
		return fromString(uri.toString());
	}
}
