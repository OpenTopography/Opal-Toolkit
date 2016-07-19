/*
 * Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id: LICENSE,v 1.8 2004/02/09 03:33:38 ian Exp $
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
 * cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
 * pioneering role in inventing and promulgating (and standardizing) the Java 
 * language and environment is gratefully acknowledged.
 * 
 * The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
 * inventing predecessor languages C and C++ is also gratefully acknowledged.
 */

package edu.sdsc.nbcr.opal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.GZIPInputStream;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarEntry;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.FaultType;

/**
 * Extract -- print or unzip a JAR or PKZIP file using java.util.zip. Command-line
 * version: extracts files.
 * 
 * @author Ian Darwin, Ian@DarwinSys.com $Id: Extract.java,v 1.7 2004/03/07
 *         17:40:35 ian Exp $
 * @author Sriram Krishnan, modified the version from http://tinyurl.com/yz5wnz2
 */
public class Extract {
    // get an instance of the log4j Logger
    private static Logger logger = 
	Logger.getLogger(Extract.class.getName());

    /** The ZipFile that is used to read an archive */
    protected ZipFile zippy;

    /** The buffer for reading/writing the ZipFile data */
    protected byte[] b = new byte[8092];

    /** Cache of paths we've mkdir()ed. */
    protected SortedSet dirsMade;

    protected boolean warnedMkDir = false;

    /** Default constructor */
    public Extract() {
	dirsMade = new TreeSet();
    }

    /** For a given Zip file, process each entry. */
    public void extract(String targetDir, 
			String fileName) 
	throws FaultType {
	logger.debug("called");

	// check for validity of zip file
	if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
	    // valid zip file - proceed to extract
	    unZipFile(targetDir, fileName);
	} else if (fileName.endsWith(".tar") || fileName.endsWith("tar.gz")) {
	    unTarFile(targetDir, fileName);
	} else if (fileName.endsWith(".gz")) {
	    gunZipFile(targetDir, fileName);
	} else {
	    String msg = "File " + fileName + 
		" doesn't end with .zip, .jar, .gz or .tar(.gz) - " +
		"can't extract";
	    logger.warn(msg);
	    return;
	}
    }


    /**
     * Extract a gzip file archive
     */
    protected void gunZipFile(String targetDir, 
			     String fileName) 
	throws FaultType {
	logger.debug("called");

	try {
	    String outputFileName = fileName.substring(0, fileName.indexOf(".gz"));
	    FileOutputStream out = new FileOutputStream(new File(outputFileName));
	    GZIPInputStream in = new GZIPInputStream(new FileInputStream(fileName));
	    
	    int MAX_BUFFER_SIZE = 1024;
	    byte[] buf = new byte[MAX_BUFFER_SIZE];
	    int len;
	    
	    while ((len = in.read(buf)) > 0 ) {
		out.write(buf, 0, len);
	    }
	    
	    out.flush();
	    out.close();
	    in.close();
	} catch (IOException ioe) {
	    String msg = "Error while extracting gzip file: " + 
		ioe.getMessage();
	    logger.error(msg);
	    throw new FaultType(msg);
	}

    }

    /**
     * Extract a zip file archive
     */
    protected void unZipFile(String targetDir, 
			     String fileName) 
	throws FaultType {
	logger.debug("called");

	try {
	    zippy = new ZipFile(fileName);
	    Enumeration all = zippy.entries();
	    while (all.hasMoreElements()) {
		unZipFileEntry(targetDir, (ZipEntry) all.nextElement());
	    }
	} catch (IOException err) {
	    logger.error(err);
	    String msg = "IO Error while unzipping input file: " + err.getMessage();
	    throw new FaultType(msg);
	} 
    }

    /**
     * Process one file from the zip, given its name. Either print the name, or
     * create the file on disk.
     */
    protected void unZipFileEntry(String targetDir, 
				  ZipEntry e) 
	throws IOException {
	String zipName = e.getName();


	if (zipName.startsWith("/")) {
	    if (!warnedMkDir)
		logger.debug("Ignoring absolute paths");
	    warnedMkDir = true;
	    zipName = zipName.substring(1);
	}
	// if a directory, just return. We mkdir for every file,
	// since some widely-used Zip creators don't put out
	// any directory entries, or put them in the wrong place.
	if (zipName.endsWith("/")) {
	    return;
	}
	// else this is a file

	// create the directory, if need be
	createDir(targetDir, zipName);

	// extract the file contents
	String fileName = targetDir + File.separator + zipName;
	logger.debug("Creating " + fileName);
	FileOutputStream os = new FileOutputStream(fileName);
	InputStream is = zippy.getInputStream(e);

	int MAX_BUFFER_SIZE = 1024;
	byte[] buf = new byte[MAX_BUFFER_SIZE];
	int len;
	    
	while ((len = is.read(buf)) > 0 ) {
	    os.write(buf, 0, len);
	}

	os.flush();
	os.close();
	is.close();
    }

    /**
     * Extract a tar file archive
     */
    protected void unTarFile(String targetDir,
			     String fileName)
	throws FaultType {
	logger.debug("called");

	try {
	    InputStream in;
	    if (fileName.endsWith(".gz")) {
		in = new GZIPInputStream(new FileInputStream(fileName));
	    } else {
		in = new FileInputStream(fileName);
	    }

	    TarInputStream tin = new TarInputStream(in);
	    TarEntry tarEntry = tin.getNextEntry();
	    while (tarEntry != null) {
		unTarFileEntry(targetDir, tin, tarEntry);
		tarEntry = tin.getNextEntry();
	    }
	    tin.close();
	} catch (IOException ioe) {
	    String msg = "Error while extracting tarball: " + 
		ioe.getMessage();
	    logger.error(msg);
	    throw new FaultType(msg);
	}
    }

    protected void unTarFileEntry(String targetDir,
				  TarInputStream tin,
				  TarEntry tarEntry)
	throws IOException {
	File destPath = new File(targetDir + 
				 File.separator + 
				 tarEntry.getName());
	logger.debug("Processing " + destPath.getAbsoluteFile());

	// create directory, if need be
	createDir(targetDir, tarEntry.getName());

	if (!tarEntry.isDirectory()) {
	    FileOutputStream fout = new FileOutputStream(destPath);
	    tin.copyEntryContents(fout);
	    fout.close();
	} else {
	    // just return, since we mkdir for every file
	}
    }

    protected void createDir(String targetDir, 
			     String zipName) {
	// Get the directory part.
	int ix = zipName.lastIndexOf('/');
	if (ix > 0) {
	    String dirName = zipName.substring(0, ix);
	    dirName = targetDir + File.separator + dirName;
	    if (!dirsMade.contains(dirName)) {
		File d = new File(dirName);
		// If it already exists as a dir, don't do anything
		if (!(d.exists() && d.isDirectory())) {
		    // Try to create the directory, warn if it fails
		    logger.debug("Creating Directory: " + dirName);
		    if (!d.mkdirs()) {
			logger.warn("Warning: unable to mkdir "
				    + dirName);
		    }
		    dirsMade.add(dirName);
		}
	    }
	}
    }

    public static void main(String[] args)
	throws Exception {

	Extract ex = new Extract();
	ex.extract("build", "./samples/samples.zip");
    }
}
