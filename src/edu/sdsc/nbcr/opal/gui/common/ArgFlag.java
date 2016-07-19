package edu.sdsc.nbcr.opal.gui.common;

import org.apache.struts.action.ActionForm;

/**
 * This class is supposed to keep the data necessary to represent a flag
 * 
 * This class has the following fields:
 * <ul>
 * <li>String id - a unique id representing this tag
 * <li>String tag - the tag used to activate this flag on the command line
 * <li>String textDesc - the textual description for this flag
 * <li>boolean selected - this field is used by struts to place the input of the user when he submit the form
 * </li>
 * 
 * @author clem
 *
 */
public class ArgFlag extends ActionForm{

	private String id;
	private String tag;
	private String textDesc;
	private boolean selected; 

	/**
	 * default constructor
	 */
    public ArgFlag(){
	    id = null;
	    tag = null;
	    textDesc = null;
	}
    
    /**
     * Parametrized constructor see at the top of the page for the information on the various field
     * 
     * @param id a unique id representing this tag
     * @param tag the tag used to activate this flag on the command line
     * @param textDesc the textual description for this flag
     */
    public ArgFlag(String id, String tag, String textDesc){
    	this.id = id;
    	this.tag = tag;
    	this.textDesc = textDesc;
    }
    
    
    /**
     * this method is called to reset the input of the user, aka selected field
     */
    public void reset(){
    	selected = false;
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
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * returns a textual representation of this ArgFlag 
     */
    public String toString(){
        return "Tag " + id + " has a tag " + tag + " (" + textDesc + ")";
    }
    
}
