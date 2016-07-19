package edu.sdsc.nbcr.opal.gui.common;


import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.sdsc.nbcr.opal.AppMetadataInputType;
import edu.sdsc.nbcr.opal.AppConfigInputType;
import edu.sdsc.nbcr.opal.AppMetadataType;
import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.FaultType;
import edu.sdsc.nbcr.opal.FlagsArrayType;
import edu.sdsc.nbcr.opal.FlagsType;
import edu.sdsc.nbcr.opal.GroupsArrayType;
import edu.sdsc.nbcr.opal.GroupsType;
import edu.sdsc.nbcr.opal.ImplicitParamsType;
import edu.sdsc.nbcr.opal.ParamType;
import edu.sdsc.nbcr.opal.ParamsArrayType;
import edu.sdsc.nbcr.opal.ParamsType;
import edu.sdsc.nbcr.opal.gui.common.AppMetadata;
import edu.sdsc.nbcr.opal.gui.common.ArgFlag;
import edu.sdsc.nbcr.opal.gui.common.ArgParam;
import edu.sdsc.nbcr.opal.gui.common.Constants;
import edu.sdsc.nbcr.opal.gui.common.Group;
import edu.sdsc.nbcr.opal.gui.common.OPALService;




/**
 * This class implement a parser, that reading metadata from the web service 
 * creates the corresponding data structure using AppMetadata.
 * 
 * Given a serviceURL it fetches the metadata from the specified service and 
 * it returns the corresponding AppMedata Object
 * 
 * @author clem
 *
 */
public class AppMetadataParser{

    private static Log log = LogFactory.getLog(Constants.PACKAGE);
    
    /**
     * give a service URL it fetches the metadata, and it parses it 
     * @param serviceURL
     * 
     * @return the AppMetadata object containing information fetched from serviceURL
     * 
     */
    static public AppMetadata parseAppMetadata(String serviceURL){
        //query the service and obtain the config file
        AppMetadata app = new AppMetadata();
        // connect to the App Web service
        AppServiceLocator asl = new AppServiceLocator();
        log.debug("PARSER: Starting the parsing process...");
        /*       ----------  this is for htts, not supported at the moment
        // register a protocol handler for https, if need be
        int index = serviceURL.indexOf(":");
        boolean httpsInUse = false;
        if (index > 0) {
            String proto = serviceURL.substring(0, index);
            if (proto.equals("https")) {
                httpsInUse = true;
            }
        }
        if (httpsInUse) {
            SimpleProvider provider = new SimpleProvider();
            SimpleTargetedChain c = new SimpleTargetedChain(new HTTPSSender());
            provider.deployTransport("https", c);
            asl.setEngine(new AxisClient(provider));
            Util.registerTransport();
            System.out.println("HTTPS protocol handler registered\n");
        } */
        try {
            AppServicePortType appServicePort = asl.getAppServicePort(new URL(serviceURL));
	    AppConfigType act = appServicePort.getAppConfig(new AppConfigInputType());
	    AppMetadataType amt = act.getMetadata();
            // AppMetadataType amt = appServicePort.getAppMetadata(new AppMetadataInputType());
      
            //setting general info
            app.setUsage(amt.getUsage());
            app.setInfo(amt.getInfo());
            app.setURL(serviceURL);
	    if (act.isParallel()) {
		app.setParallel(true);
	    }
            String serviceName = amt.getAppName();
            if ( serviceName == null) {
                serviceName =  serviceURL.substring(serviceURL.lastIndexOf("/") + 1, serviceURL.length());
            }
            app.setServiceName(serviceName);
            
            if (amt.getTypes() != null){
                //if Types is not present is pointless going on with the parsing...
                
                //setting flags
                if ((amt.getTypes().getFlags() != null) && ( amt.getTypes().getFlags().getFlag() != null ) ){
                    FlagsType [] flagsType = amt.getTypes().getFlags().getFlag();
                    
                    ArgFlag [] flags = new ArgFlag[flagsType.length];
                    for (int i = 0; i < flagsType.length; i++ ){
                        flags[i] = parseFlag(flagsType[i]);
                    }
                    app.setArgFlags(flags);
                } else { 
                    log.debug("PARSER: there are no flag in the parsed config file");
                    app.setArgFlags(null); 
                }
                
                //setting tagged parameters
                ParamsType [] paramsType = null;
                ArrayList args = new ArrayList();
                ParamsArrayType taggedArrayType = amt.getTypes().getTaggedParams();
                if ( (taggedArrayType != null) && (taggedArrayType.getParam() != null ) ){
                    app.setSeparator( taggedArrayType.getSeparator() );
                    paramsType = taggedArrayType.getParam();
                    for ( int i = 0; i < paramsType.length; i++ ) {
                        args.add(parseParam(paramsType[i], -1));
                    }
                } else {
                    log.debug("PARSER: There are not tagged parameters in the parsed file");
                }
                
                //setting untagged parameters
                ParamsArrayType untaggedArrayType = amt.getTypes().getUntaggedParams() ;
                if ( (untaggedArrayType != null) && ( untaggedArrayType.getParam() != null) ){
                    paramsType = untaggedArrayType.getParam();
                    for ( int i = 0; i < paramsType.length; i++ )
                        args.add(parseParam(paramsType[i], i));
                } else {
                    log.debug("PARSER: there are not untagged paramters in the parsed file");
                }
                if ( (taggedArrayType != null) || (untaggedArrayType != null) ) {
                    app.setArgParams( (ArgParam[]) args.toArray(new ArgParam[args.size()]));
                }

                //TODO handle implicit param!!!!
                //ImplicitParamsType [] implicitParams = amt.getTypes().getImplicitParams().getParam();
                
                //setting groups 
                GroupsArrayType groupsArrayType = amt.getTypes().getGroups();
                GroupsType [] groupsType = null;
                ArrayList groups = new ArrayList();
                if ( (groupsArrayType != null) && (groupsArrayType.getGroup() != null) ) {
                        log.debug("PARSER: parsing groups");
                        groupsType = groupsArrayType.getGroup();
                        //ok we have groups
                
                        for ( int i = 0; i < groupsType.length; i++ ) {
                                groups.add( parseGroup(groupsType[i], app));
                        }//for
                } else { 
                    log.debug("PARSER: there are no group in the parsed config file setting up the default one");
                }//group
                if (  ! setDefaultGroup(groups, app) ) { 
                        //something went wrong currently impossible!!!
                        log.error("An error occurred while setting the default group.");
                }//if
                app.setGroups((Group[]) groups.toArray(new Group[groups.size()]));
            }
            else log.debug("PARSER: no types found in the config file");
            log.debug("PARSER: ---  Parsed sucesfully the configuration file    --\n" + app + "PARSER:         ------------        ");
        }catch (FaultType e){
            //some error fetching the data from the remote server
            log.error("PARSER: Unable to get the appMetadata from the Opal server. Error: " + e.getMessage1());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            //some error fetching the data from the remote server
            log.error("PARSER: Unable to get the appMetadata from the Opal server. Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        //TODO reorder the data structure before displaying 
        //app = reoderAppMetadata(app);
        return app;
    }//if types
    
    /**
     * Parse a FlagsType and return the corresponding ArgFlag. This class is only a converter.
     * 
     * @param flagType
     * @return the corresponding ArgFlag
     * 
     * @see import edu.sdsc.nbcr.opal.gui.common.ArgFlag
     */
    static private ArgFlag parseFlag(FlagsType flagType){
        ArgFlag flag = new ArgFlag();
        flag.setId(flagType.getId().toString());
        flag.setTag(flagType.getTag());
        flag.setTextDesc(flagType.getTextDesc());
        Boolean defaultValue = flagType.get_default();
        if (defaultValue != null) flag.setSelected(defaultValue.booleanValue());
        return flag;
    }

    /**
     * Parse a paramType and return a ArgParam. This class is only a converter.
     * 
     * @param paramType 
     * @param position the current index for un-tagged parameters
     * 
     * @return the corresponding ArgParam
     * 
     * @see import edu.sdsc.nbcr.opal.gui.common.ArgParam
     */
    static private ArgParam parseParam(ParamsType paramType, int position){
        log.debug("parsing the paramType: " + paramType.getTag());
        ArgParam arg = new ArgParam();
        arg.setId(paramType.getId().toString());
        arg.setTag(paramType.getTag());
        arg.setTextDesc(paramType.getTextDesc());
        String defaultValue = paramType.get_default();
        if ( (defaultValue != null) && (defaultValue.length() != 0) )
                //setting the default values
                arg.setSelectedValue(defaultValue);
        if (paramType.getParamType() != null) arg.setType(paramType.getParamType().getValue());
        if (paramType.getIoType() != null )  arg.setIoType(paramType.getIoType().getValue());
        arg.setSemanticType(paramType.getSemanticType());
        arg.setValues(paramType.getValue());
        if ( paramType.getRequired() != null ) {
                //System.out.println("the param " + arg.getId() + " is required " + paramType.getRequired());
                arg.setRequired(paramType.getRequired().booleanValue());
        }
        arg.setPosition(position);
        return arg;
    }
    
    /**
     * It parses the groupType and it returns a corresponding Group instance. 
     * This class is only a converter.
     * 
     * @param groupType the groupType to be parsed
     * @param app the current AppMetadata. AppMetadata has to be already loaded with the ArgParams and ArgFlags
     * 
     * @return the corresponding Group instance 
     */
    static private Group parseGroup(GroupsType groupType, AppMetadata app){
        boolean required = false;
        Group group = new Group();
        group.setName(groupType.getName());
        if ( groupType.getExclusive() != null ) group.setExclusive(groupType.getExclusive().booleanValue());
        if ( groupType.getRequired() != null ) {
                required = groupType.getRequired().booleanValue();
                group.setRequired(required);
        }
        group.setTextDesc(groupType.getTextDesc());
        group.setSemanticType(groupType.getSemanticType());
        //let's built the flags and parameters array
        ArrayList params = new ArrayList();
        ArrayList flags = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(groupType.getElements().toString());
        while ( tokenizer.hasMoreTokens() ) {
                String id = tokenizer.nextToken();
                if ( app.getArgFlagId(id) != null ) {
                        flags.add(app.getArgFlagId(id));
                        continue;
                }
                if ( app.getArgParamId(id) != null ) {
                        //if the group is required we make all the members required...
                        if (required) app.getArgParamId(id).setRequired(required);
                        params.add(app.getArgParamId(id));
                        continue;
                }
                log.error("The parameters idref " + id +  " of the group " + group.getName() + " is available nor in the flags neither in the params!!");
        }//while
        if ( !flags.isEmpty() ) group.setArgFlags( (ArgFlag[]) flags.toArray(new ArgFlag[flags.size()]) );
        if ( !params.isEmpty() ) group.setArgParams( (ArgParam[]) params.toArray(new ArgParam[params.size()]) );
        return group;
    }
    
    /**
     * This function creates another group which contains all the 
     * flags and parameters that have not already been included in 
     * all the other groups
     *  
     * @param groups the current list of groups
     * @param app the current appMetadata with all the Group, argParam and ArgFlag loaded
     * @return always true
     */
    static private boolean setDefaultGroup(ArrayList groups, AppMetadata app){
        ArgParam [] ungroupedParams = getUngroupedParams( groups, app.getArgParams());
        ArgFlag [] ungroupedFlags = getUngroupedFlags(groups, app.getArgFlags());
        if ( (ungroupedParams.length == 0) && (ungroupedFlags.length == 0) ) {
                log.debug("There are no parameters or flags ungrouped...");
                return true;
        }
        String info = "PARSER: the ungropped paramters are: ";
        for (int i = 0; i< ungroupedParams.length; i++)
                info += ungroupedParams[i].getId() + " ";
        info += "\nPARSER: the ungroupped flags are: ";
        for (int i = 0; i< ungroupedFlags.length; i++)
                info += ungroupedFlags[i].getId() + " ";
        log.debug(info);
        Group defaultGroup = new Group();
        defaultGroup.setName("Default Group");
        defaultGroup.setTextDesc("Ungrouped input fields...");
        defaultGroup.setArgFlags(ungroupedFlags);
        defaultGroup.setArgParams(ungroupedParams);
        groups.add(defaultGroup);
        return true;
    }
    
    /**
     * Returns an array of ArgParam containing all the params that have not already 
     * been included in another group
     * 
     */
    static private ArgParam []  getUngroupedParams(ArrayList groups, ArgParam [] params){
        if ( params == null ) return new ArgParam[0];
        ArrayList paramList = new ArrayList(Arrays.asList(params));
    
        for (int i = 0; i < groups.size(); i++) {
                ArgParam [] usedParams = ((Group) groups.get(i)).getArgParams();
                if (usedParams != null ){
                        for (int t = 0; t < usedParams.length; t++) 
                                paramList.remove(usedParams[t]);
                }//for
        }//for
        return (ArgParam []) paramList.toArray(new ArgParam[paramList.size()]);
    }
    
    /**
     * returns an array of ArgFlags containing all the flags that have not been already used in another goup 
     * @param groups
     * @param flags
     * 
     */
    static private ArgFlag [] getUngroupedFlags(ArrayList groups, ArgFlag [] flags){
        if ( flags == null ) return new ArgFlag[0];
        ArrayList flagsList = new ArrayList(Arrays.asList(flags));
    
        for ( int i = 0; i < groups.size(); i++ ) {
                ArgFlag [] usedFlags = ((Group) groups.get(i)).getArgFlags();
                if ( usedFlags != null ) {
                        for (int t = 0; t < usedFlags.length; t++ ) 
                                flagsList.remove(usedFlags[t]);
                }//for
        }//for
        return (ArgFlag []) flagsList.toArray(new ArgFlag[flagsList.size()]);
    }
    
    

    /**
     * Use this function if you want to create an AppMetadata data 
     * structure without having to parse the XML file. For development only.
     * @return a static AppMetadata
     */
    static private AppMetadata createStaticAppMetadata(){
        AppMetadata app = new AppMetadata();
        String [] str = {"man page"};
        app.setInfo(str);
        app.setUsage("python pdb2pqr.py [options] --ff={forcefield} {path} {output-path}");
        app.setServiceName("PDB2PQR");
        app.setURL("http://localhost:8080/axis/services/PDB2PQR");
        ArgFlag [] flags = new ArgFlag[5];
        flags[0] = new ArgFlag("nodebump", "--nodebump", "Do not perform the debumping operation");
        flags[1] = new ArgFlag("noopt","--noopt", "Do not perform hydrogen bonding network optimization");
        flags[2] = new ArgFlag("clean", "--clean", "Do no optimization, atom addition, or parameter assignment");
        flags[3] = new ArgFlag("chain", "--chain", "Keep the chain ID in the output PQR file");
        flags[4] = new ArgFlag("assign-only", "--assign-only", "Only assign charges and radii - do not add atoms, debump, or optimize");
        /*        
        flags[5] = new ArgFlag("apbs-input", "--apbs-input", "Create a template APBS input file based on the generated PQR file");
        flags[6] = new ArgFlag("chi", "--chi", "Print the per-residue backbone chi angle to {output-path}.chi");
        flags[7] = new ArgFlag("phi", "--phi", "Print the per-residue backbone phi angle to {output-path}.phi");
        flags[8] = new ArgFlag("rama", "--rama", "Print the per-residue phi and psi angles to {output-path}.rama for Ramachandran plots");
        flags[9] = new ArgFlag("hbond", "--hbond", "Print a list of hydrogen bonds to {output-path}.hbond");
        */

        app.setArgFlags(flags);
        String [] values = { "AMBER", "CHARMM", "PARSE", "TYL06"};
        ArgParam [] args = new ArgParam[6];
        args[0] = new ArgParam("path", null, "FILE", "INPUT", true, null, null,
                "Input PDB file *");
        args[1] = new ArgParam("forcefield", "--ff", "STRING", "INPUT", true, values, null, 
                "The forcefield to use -- currently AMBER, CHARMM, PARSE, and TYL06 are supported *");
        args[2] = new ArgParam("output-path", null, "STRING", "OUTPUT", false, null, null, 
                "The desired output name of the PQR file to be generated");
        args[3] = new ArgParam("ffout", "--ffout", "STRING", null, false, null, null, 
                "Instead of using the standard canonical naming scheme, use the names from the given forcefield");
        args[4] = new ArgParam("with-ph", "--with-ph", "STRING", null, false, null, null, 
                "Use PROPKA to calculate pKas and apply them to the molecule given the pH value");
        args[5] = new ArgParam("ligand", "--ligand", "FILE", "OUTPUT", false, null, null,
                "Use the PDB2PKA package to generate parameters for the specific ligand in MOL2 format");
        app.setArgParams(args);
        app.setSeparator("=");
        //set all the parameters
        return app;
    }

} 
