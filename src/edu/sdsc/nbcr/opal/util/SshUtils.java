package edu.sdsc.nbcr.opal.util;

import java.io.*;

import com.jcraft.jsch.*;

import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.manager.JobManagerException;
import org.apache.log4j.Logger;
// import sun.tools.jstat.Jstat;

public class SshUtils {

    private static Logger logger = Logger.getLogger(SshUtils.class.getName());
    private String user;
    private String host;
    private int port;
    private String keyFilePath;
    private String keyPassword;
    private Session session = null;
    private int timeOut = -1;
    private ChannelSftp channelsftp;

    public SshUtils(String user,
                    String host,
                    int port,
                    String keyFilePath,
                    String keyPassword) throws JobManagerException {
        logger.info("called");
        this.user = user;
        this.host = host;
        this.port = port;
        this.keyFilePath = keyFilePath;
        this.keyPassword = keyPassword;
        //this.session = createSession();
    }

    public void setSessionTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    private ChannelSftp setChannelSFTP(Session session) throws JobManagerException {
        logger.info("creating channel for sftp");
        ChannelSftp channelsftp = null;
        try {
            Channel c = session.openChannel("sftp");
            c.connect();
            channelsftp = (ChannelSftp) c;
            logger.info("channel sftp connected !!!");
        } catch (JSchException e) {
            throw new JobManagerException(e.getMessage());
        }
        return channelsftp;
    }

    private Session createSession(String type) throws JobManagerException {
        // logger.info("creating session");
        Session session = null;
        int numoftrials = 0;
        String error_msg = "";

        while (numoftrials < 20) {
            try {
                JSch jsch = new JSch();
                jsch.addIdentity(keyFilePath, keyPassword);

                Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");

                session = jsch.getSession(user, host, port);
                session.setConfig(config);
                if (timeOut == -1)
                    session.connect();
                else
                    session.connect(timeOut);

                break;
            } catch (JSchException e) {
                //throw new JobManagerException(e.getMessage());
                error_msg = "Calling function type = " + type + " Connection error - " + e.getMessage();
                logger.info(error_msg);
                if (e.toString().contains("Auth fail")) {
                    numoftrials++;
                    logger.info("Calling function type = " + type + " Retry to authenticate..." + numoftrials);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex) {
                        logger.info("Calling function type = " + type + " " + ex.getMessage());
                    }
                } else if (e.toString().contains("Connection refused")) {
                    numoftrials++;
                    logger.info("Calling function type = " + type + " Retry to authenticate..." + numoftrials);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex) {
                        logger.info("Calling function type = " + type + " " + ex.getMessage());
                    }
                } else {
                    throw new JobManagerException(error_msg);
                }
            }
        }

        if(numoftrials == 20) throw new JobManagerException(error_msg);

        return session;
    }

    public Session getSession() {
        return this.session;
    }

    public void sftp_close() {
        //channelsftp.quit();
        channelsftp.exit();
        channelsftp.disconnect();
        session.disconnect();
    }

    public String sendCommand(String cmd)
            throws JobManagerException {
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder errorBuffer = new StringBuilder();

        try {
            setSessionTimeOut(60000);
            Session session = createSession("sendCommand");
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            InputStream commandOutput = channel.getInputStream();
            InputStream errorOut = channel.getErrStream();
            channel.connect();

            int stdout = commandOutput.read();
            while (stdout != 0xffffffff) {
                outputBuffer.append((char) stdout);
                stdout = commandOutput.read();
            }

            int stderr = errorOut.read();
            while (stderr != 0xffffffff) {
                errorBuffer.append((char) stderr);
                stderr = errorOut.read();
            }

            int exitStatus = channel.getExitStatus();

            channel.disconnect();
            session.disconnect();

            if (exitStatus != 0)
                logger.info("Done, but with error! " + exitStatus);
            /*
            if (exitStatus < 0) {
                logger.info("Done, but exit status not set!");
            } else if (exitStatus > 0) {
                logger.info("Done, but with error!");
            } else {
                //logger.info("Done!");
                if (outputBuffer.toString().isEmpty())
                    return "DONE";
            }
            */

        } catch (IOException | JSchException ioX) {
            throw new JobManagerException(ioX.toString());
        }

        String job_status = outputBuffer.toString();
        if (job_status.isEmpty())
            job_status = "DONE";

        if (!errorBuffer.toString().isEmpty()) {
            logger.info("Error stream: " + errorBuffer.toString());
            job_status = "DONE";
        }

        return job_status;
    }

    public void transferFileToRemote(String localFile,
                                     String remoteDir)
            throws JobManagerException {
        logger.info("tranfer file to remote: " + localFile);
        try {
            Session session = createSession("transferFileToRemote");
            ChannelSftp channelsftp = setChannelSFTP(session);
            channelsftp.mkdir(remoteDir);
            channelsftp.cd(remoteDir);
            channelsftp.put(new FileInputStream(new File(localFile)), new File(localFile).getName(), ChannelSftp.OVERWRITE);

            channelsftp.exit();
            //channelsftp.disconnect();
            session.disconnect();
        } catch (SftpException | FileNotFoundException ex) {
            throw new JobManagerException(ex.getMessage());
        }
    }

    public void transferFilesToLocal(String remoteDir,
                                     String[] filters,
                                     String localDir)
            throws JobManagerException {
        logger.info("tranfer file to local: " + Arrays.toString(filters));
        try {
            Session session = createSession("transferFilesToLocal");
            ChannelSftp channelsftp = setChannelSFTP(session);
            channelsftp.cd(remoteDir);

            Vector list = channelsftp.ls("*");
            for (Object entry : list) {
                String filename = ((ChannelSftp.LsEntry) entry).getFilename();
                for (String filter : filters) {
                    if (filename.endsWith(filter)) {
                        logger.info("Filename: " + filename);
                        String filepath = localDir + File.separator + filename;
                        channelsftp.get(filename, filepath);
                        break;
                    }
                }
            }

            channelsftp.exit();
            //channelsftp.disconnect();
            session.disconnect();
        } catch (SftpException ex) {
            throw new JobManagerException(ex.getMessage());
        }
        logger.info("tranfer file to local: Done!");
    }

    private void transferDirToRemote(String localDir,
                                     String remoteDir)
            throws JobManagerException {

        try {
            logger.trace("local dir: " + localDir + ", remote dir: " + remoteDir);

            File localFile = new File(localDir);
            channelsftp.cd(remoteDir);

            // for each file  in local dir
            for (File localChildFile : localFile.listFiles()) {

                // if file is not dir copy file
                if (localChildFile.isFile()) {
                    logger.trace("file : " + localChildFile.getName());
                    transferFileToRemote(localChildFile.getAbsolutePath(), remoteDir);

                } // if file is dir
                else if (localChildFile.isDirectory()) {

                    // mkdir  the remote
                    SftpATTRS attrs;
                    try {
                        attrs = channelsftp.stat(localChildFile.getName());
                    } catch (Exception e) {
                        channelsftp.mkdir(localChildFile.getName());
                    }

                    logger.trace("dir: " + localChildFile.getName());

                    // repeat (recursive)
                    transferDirToRemote(localChildFile.getAbsolutePath(), remoteDir + "/" + localChildFile.getName());
                    channelsftp.cd("..");
                }
            }
        } catch (SftpException ex) {
            throw new JobManagerException(ex.getMessage());
        }

    }

    public void transferToLocal(String remoteDir,
                                String remoteFile,
                                String localDir)
            throws JobManagerException {

        try {
            channelsftp.cd(remoteDir);
            byte[] buffer = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(channelsftp.get(remoteFile));

            File newFile = new File(localDir);
            OutputStream os = new FileOutputStream(newFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);

            logger.trace("writing files ...");
            int readCount;
            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }
            logger.info("completed !!!");
            bis.close();
            bos.close();

        } catch (SftpException | IOException ex) {
            throw new JobManagerException(ex.getMessage());
        }
    }

    public void copyRemoteToLocal(Session session,
                                  String from,
                                  String to,
                                  String fileName) throws JobManagerException {
        try {
            from = from + File.separator + fileName;
            String prefix = null;

            if (new File(to).isDirectory()) {
                prefix = to + File.separator;
            }

            // exec 'scp -f rfile' remotely
            String command = "scp -f " + from;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] buf = new byte[1024];

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            while (true) {
                int c = checkAck(in);
                if (c != 'C') {
                    break;
                }

                // read '0644 '
                in.read(buf, 0, 5);

                long filesize = 0L;
                while (true) {
                    if (in.read(buf, 0, 1) < 0) {
                        // error
                        break;
                    }
                    if (buf[0] == ' ') break;
                    filesize = filesize * 10L + (long) (buf[0] - '0');
                }

                String file = null;
                for (int i = 0; ; i++) {
                    in.read(buf, i, 1);
                    if (buf[i] == (byte) 0x0a) {
                        file = new String(buf, 0, i);
                        break;
                    }
                }

                System.out.println("file-size=" + filesize + ", file=" + file);

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                // read a content of lfile
                FileOutputStream fos = new FileOutputStream(prefix == null ? to : prefix + file);
                int foo;
                while (true) {
                    if (buf.length < filesize) foo = buf.length;
                    else foo = (int) filesize;
                    foo = in.read(buf, 0, foo);
                    if (foo < 0) {
                        // error
                        break;
                    }
                    fos.write(buf, 0, foo);
                    filesize -= foo;
                    if (filesize == 0L) break;
                }

                if (checkAck(in) != 0) {
                    System.exit(0);
                }

                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                try {
                    fos.close();
                } catch (Exception ex) {
                    throw new JobManagerException(ex.getMessage());
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException ex) {
            throw new JobManagerException(ex.getMessage());
        }

    }

    public void copyLocalToRemote(Session session,
                                  String from,
                                  String to,
                                  String fileName) throws JobManagerException {
        try {
            boolean ptimestamp = true;
            from = from + File.separator + fileName;

            // exec 'scp -t rfile' remotely
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + to;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            File _lfile = new File(from);

            if (ptimestamp) {
                command = "T" + (_lfile.lastModified() / 1000) + " 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();
                if (checkAck(in) != 0) {
                    System.exit(0);
                }
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = _lfile.length();
            command = "C0644 " + filesize + " ";
            if (from.lastIndexOf('/') > 0) {
                command += from.substring(from.lastIndexOf('/') + 1);
            } else {
                command += from;
            }

            command += "\n";
            out.write(command.getBytes());
            out.flush();

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send a content of lfile
            FileInputStream fis = new FileInputStream(from);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) break;
                out.write(buf, 0, len); //out.flush();
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            if (checkAck(in) != 0) {
                System.exit(0);
            }
            out.close();

            try {
                fis.close();
            } catch (Exception ex) {
                throw new JobManagerException(ex.getMessage());
            }

            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException ex) {
            throw new JobManagerException(ex.getMessage());
        }
    }


    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //         -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                logger.info(sb.toString());
            }
            if (b == 2) { // fatal error
                logger.info(sb.toString());
            }
        }
        return b;
    }
}
