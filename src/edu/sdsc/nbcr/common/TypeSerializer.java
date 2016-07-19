package edu.sdsc.nbcr.common;

import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;

/**
 * A utility class that serializes an Axis generated object into XML
 *
 * @author Brent Stearn
 */
public class TypeSerializer {

    // get an instance of the log4j logger
    private static Logger logger =
	Logger.getLogger(TypeSerializer.class.getName());
    
    /**
     * Sole constructor
     */
    public TypeSerializer() {
    }
    
    /**
     * Method to serialize an Axis generated object into XML
     * 
     * @param path fully qualified path for the file to be written to.
     * Will overwrite if file already exists
     * 
     * @param qname the qualified name for the top level element
     * @param ret instance of type to be written
     */
    public static void writeValue( String path, QName qname, Object ret ) 
	throws NoSuchMethodException, 
	       IllegalAccessException, 
	       InvocationTargetException,
	       IOException {

	logger.debug("called");
	
	// System.out.println(ret.toString());
        
	// java.io.StringWriter sw = new java.io.StringWriter();
	FileWriter fw = new FileWriter( path );
	
	Class retClass = ret.getClass();
        
	// simulating
	// org.apache.axis.encoding.Serializer ser =
	// val.getSerializer("", val.getClass(), val.getTypeDesc().getXmlType());
        
	// get TypeDesc object
	Method getTypeDesc = retClass.getDeclaredMethod( "getTypeDesc", (Class[]) null );
	Object typeDescRet = getTypeDesc.invoke( ret, (Object[]) null );
            
	// System.err.println( getTypeDesc.toString() );
            
	// get xmlType (QName) object
	Method getXmlType = typeDescRet.getClass().getMethod( "getXmlType", (Class[]) null );
	Object xmlTypeRet = getXmlType.invoke( typeDescRet, (Object[]) null );

	// System.err.println( getXmlType.toString() );
        
	// call getSerializer method
	// args: String mechType, Class javaType, QName xmlType
	// empty string is the value for "mechType" parameter
	String mechType = org.apache.axis.Constants.AXIS_SAX;
	Class[] classes = new Class[ 3 ];
	classes[0] = mechType.getClass();
	classes[1] = mechType.getClass().getClass();
	classes[2] = xmlTypeRet.getClass();
	Method getSerializer = retClass.getMethod( "getSerializer",
						   classes );
	
	org.apache.axis.encoding.Serializer ser =
	    (org.apache.axis.encoding.Serializer) 
	    getSerializer.invoke(retClass, 
				 new Object[]{ mechType, retClass, xmlTypeRet } );
	
	org.apache.axis.MessageContext mc = 
	    org.apache.axis.MessageContext.getCurrentContext();
	if (mc == null)
	    mc = new org.apache.axis.MessageContext(new org.apache.axis.client.AxisClient());

	org.apache.axis.encoding.SerializationContext sc = 
	    new org.apache.axis.encoding.SerializationContext(fw, mc);
	
 	sc.setDoMultiRefs(false);
 	sc.setPretty(true);

	sc.serialize(qname, null, ret, (QName) xmlTypeRet, new Boolean(true), new Boolean(true));
	fw.close();
        
	// System.out.println(sw.toString());
    }
}
