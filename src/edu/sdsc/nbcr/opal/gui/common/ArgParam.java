package edu.sdsc.nbcr.opal.gui.common;

import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;

/**
 * This class represents a tagged or an untagged parameters. 
 * If the tag field is null then the parameters is untagged 
 * otherwise is tagged.<br/>
 * 
 * Position is needed for the untagged parameters, when we need to built 
 * the command line<br/>
 * 
 * This bean has the following fields:
 * <ul>
 * <li>String id - a unique id representing this field
 * <li>String tag - the tag used to pass this parameter on the command line, it is null if this is a untagged param
 * <li>String type - the type of this parameter (see wsdl of opal for more info)
 * <li>String ioType - the ipType of this parameter (see  wsdl of opal for more info)
 * <li>int position - the position on the command line (used only for untagged param)
 * <li>boolean required - true if this is a required param
 * <li>String [] values - if it is a multiple choice param this array contains the various possibilities
 * <li>String semanticType - not used at the moment
 * <li>String textDesc - it holds the textual description of this parameters
 * <li>String selectedValue - used by strutus to put the input of the user when he submits the form, it also holds the default value if specified
 * <li>FormFile file - used by strutus to put a file if this parameter is an input file
 * </ul> 
 *  
 * @author clem
 *
 */
public class ArgParam extends ActionForm{
    
	private String id;
	private String tag;
	private String type;
	private String ioType;
	private int position;
	private boolean required;
	private String [] values;
	private String semanticType;
	private String textDesc;
	//these are to hold the values from the form
	private String selectedValue;
	
	private FormFile file;
	
	/**
	 * default constructor
	 */
	public ArgParam(){
	    position = -1;
	    id = null;
	    tag =  null;
	    type = null;
	    ioType = null;
	    required = false;
	    values = null;
	    semanticType = null;
	    textDesc = null;
	    selectedValue = null;
	    file = null;
	}
	
	/**
	 * Parametrized constructor see at the top of the page for the information on the various field
	 * 
	 */
    public ArgParam(String id, String tag, String type, String ioType,
            boolean required, String [] values, String semaricType, String textDesc) {
        super();
        file = null;
        position = -1;
        this.id = id;
        this.tag = tag;
        this.type = type;
        this.ioType = ioType;
        this.required = required;
        this.values = values;
        this.semanticType = semaricType;
        this.textDesc = textDesc;
    }
    
    /**
     * Reset the value inputed by the user
     */
    public void reset(){
    	file = null;
    	selectedValue = null;
    }
    
    /**
     * Return true if a file has been uploaded
     * @return true if a file has been uploaded
     */
    public boolean isFileUploaded(){
    	return (file != null) && (file.getFileName().length() > 0);
    }
    
    /**
     * it returns a textual representation of this instance
     * 
     */
    public String toString(){
        String str = "Param " + id + " is tagged with " + tag + ", type is: " + type + "/" + ioType + ", ";
        if (required) str += "is required, ";
        else str += "is not required, ";
        if (values != null) {
            str += "possible value are: ";
            for (int i = 0; i < values.length; i++)
                str += values[i] + ", ";
        }
        if (selectedValue != null) str += " selected value: " + selectedValue + ", ";
        if (file != null) str += " the file is: " + file.getFileName() + ", ";
        if (tag == null) str += " its position is " + position;
        return str;
    }

	
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the ioType
     */
    public String getIoType() {
        return ioType;
    }

    /**
     * @param ioType the ioType to set
     */
    public void setIoType(String ioType) {
        this.ioType = ioType;
    }

    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(int position) {
        this.position = position;
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
     * @return the values
     */
    public String[] getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(String[] values) {
        this.values = values;
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
     * @return the selectedValue
     */
    public String getSelectedValue() {
        return selectedValue;
    }

    /**
     * @param selectedValue the selectedValue to set
     */
    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    /**
     * @return the file
     */
    public FormFile getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(FormFile file) {
        this.file = file;
    }


}
