package edu.sdsc.nbcr.opal.gui.common;


import java.io.Serializable;


/**
 * This class is used to group more ArgFlag or ArgParam together.
 * 
 * 
 * This class has the following fields:
 * <ul>
 * <li>ArgFlag [] argFlags - contains references to the ArgFlags that are part of this group
 * <li>ArgParam [] argParams - contains references to the ArgParams that are part of this group
 * <li>String name - the name of this group
 * <li>boolean exclusive - true if this group is exclusive
 * <li>boolean required - true if all the elements of this group are required
 * <li>String textDesc - a textual description of this group
 * <li>String semanticType - semantic description for this group
 * </ul>
 * 
 * @author clem
 *
 */

public class Group implements Serializable{
	
	private ArgFlag [] argFlags;
	private ArgParam [] argParams;
	private String name;
	private boolean exclusive;
	private boolean required;
	private String textDesc;
	private String semanticType;
	
	
	/**
	 * default constructor
	 */
	public Group() {
		
		this.argFlags = null;
		this.argParams = null;
		this.name = null;
		this.exclusive = false;
		this.required = false;
		this.textDesc = null;
		this.semanticType = null;
	}
	
	/**
	 * return a textual representatiion of this string
	 */
	public String toString(){
		String str = "Group " + name + " ";
		if (exclusive == true ) str += "is exclusive ";
		else str += "is not exclusive ";
		if (required == true ) str += "is required ";
		else str += "is not required ";
		str += " and is description is: " + textDesc;
		if (argParams != null ){
			str += "\n    Its params are: ";
			for (int i = 0; i < argParams.length; i++ )
				str += argParams[i].getId() + " ";
		}//if
		if (argFlags != null ){
			str += "\n    Its flags are: ";
			for (int i = 0; i < argFlags.length; i++ )
				str += argFlags[i].getId() + " ";
		}//if
		return str;
	}


	   
    //         -----------------------------
    //               Getter and Setter
    //         -----------------------------
    
	/**
     * @return the argFlags
     */
    public ArgFlag[] getArgFlags() {
        return argFlags;
    }


    /**
     * @param argFlags the argFlags to set
     */
    public void setArgFlags(ArgFlag[] argFlags) {
        this.argFlags = argFlags;
    }


    /**
     * @return the argParams
     */
    public ArgParam[] getArgParams() {
        return argParams;
    }


    /**
     * @param argParams the argParams to set
     */
    public void setArgParams(ArgParam[] argParams) {
        this.argParams = argParams;
    }


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }


    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * @return the exclusive
     */
    public boolean isExclusive() {
        return exclusive;
    }


    /**
     * @param exclusive the exclusive to set
     */
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }


    /**
     * @return the required
     */
    public boolean isRequired() {
        return required;
    }


    /**
     * @param required the required to set
     */
    public void setRequired(boolean required) {
        this.required = required;
    }


    /**
     * @return the textDesc
     */
    public String getTextDesc() {
        return textDesc;
    }


    /**
     * @param textDesc the textDesc to set
     */
    public void setTextDesc(String textDesc) {
        this.textDesc = textDesc;
    }


    /**
     * @return the semanticType
     */
    public String getSemanticType() {
        return semanticType;
    }


    /**
     * @param semanticType the semanticType to set
     */
    public void setSemanticType(String semanticType) {
        this.semanticType = semanticType;
    }


}
