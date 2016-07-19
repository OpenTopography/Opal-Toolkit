package edu.sdsc.nbcr.opal.util;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.globus.ftp.GridFTPClient;
import org.globus.util.GlobusURL;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.ftp.GridFTPSession;
import org.globus.ftp.FileInfo;

import org.apache.log4j.Logger;

import java.io.File;

/**
 *
 * Implementation of an Grid FTP test class
 */
public class GridftpTest {

    /**
     * Main method to test gridftp
     */
    public static void main(String[] args)
	throws Exception {
	
	GlobusCredential globusCred = 
	    new GlobusCredential("/Users/sriramkrishnan/certs/apbs_service.cert.pem", 
				 "/Users/sriramkrishnan/certs/apbs_service.privkey");
	GSSCredential gssCred = 
	    new GlobusGSSCredentialImpl(globusCred,
					GSSCredential.INITIATE_AND_ACCEPT);
	
	GridFTPClient client = new GridFTPClient("rocks-106.sdsc.edu",
						 2811);
	
	client.authenticate(gssCred);
	// client.makeDir("scratch/Test123");
	client.setPassive();
	client.setLocalActive();

	File inputDir = new File("/Users/sriramkrishnan/Desktop/Test123");
	String[] files = inputDir.list();
	System.out.println("Attempting to put files using GridFTP");
	for (int i = 0; i < files.length; i++) {
	    System.out.println("Uploading file: " + files[i]);

// 	    client.put(new File("/Users/sriramkrishnan/Desktop/Test123/" + files[i]),
// 		       "scratch/Test123/" + files[i],
// 		       true);


	    UrlCopy uc = new UrlCopy();
	    uc.setDestinationUrl(new GlobusURL("gsiftp://rocks-106.sdsc.edu:2811/scratch/Test123/" +
					       files[i]));
	    uc.setSourceUrl(new GlobusURL("file:////Users/sriramkrishnan/Desktop/Test123/" +
					  files[i]));
	    uc.setCredentials(gssCred);
	    uc.copy();
	}

	// copy stuff back
	System.out.println("Attempting to get files using GridFTP");
	client.changeDir("scratch/Test123");
	Object[] remoteFiles = client.list().toArray();
	for (int i = 0; i < remoteFiles.length; i++) {
	    FileInfo fileInfo = (FileInfo) remoteFiles[i];
	    if (fileInfo.isFile()) {
		String fileName = fileInfo.getName();
		System.out.println("Downloading file: " + fileName);
		UrlCopy uc = new UrlCopy();
		uc.setSourceUrl(new GlobusURL("gsiftp://rocks-106.sdsc.edu:2811/scratch/Test123/" +
					      fileName));
		uc.setDestinationUrl(new GlobusURL("file:////Users/sriramkrishnan/Desktop/Test123-output/" +
						   fileName));
		uc.setCredentials(gssCred);
		uc.copy();
	    }
	}
    }
}
