package com.se525.team5;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * @author Team 5 SE 525 Software Security Architecture Android - Final Project
 *         JschExec.java
 */
public class JschExec {

	long starttime;
	long interval = 0;
    /**
     * Constructor takes the textview to write to after the thread has completed
     * and the handler to accept callbacks from
     */
    public JschExec() {
    }

    /**
     * New thread created to perform outside of the UI interface Accepts an
     * array of Strings, expected order: [0] - Identity [1] - SSH host / host IP
     * [1] - SSH user [2] - SSH port
     * @throws Exception 
     */
    protected boolean connectandGetFile(String... args) throws Exception {
        PublicKeyTarget target = new PublicKeyTarget(args[1], args[2], args[3]);
        String identity = args[0]; // Identity of this PC
        try {
            final JSch jsch = new JSch();
            JSch.setConfig("StrictHostKeyChecking", "no");
            final Session session = target.getSession(jsch);
            boolean fileFound = false;

            try {
				session.connect();

				// SSH SFTP channel setup
				System.out.println("Starting channel");
				final ChannelSftp channel = (ChannelSftp) session
						.openChannel("sftp");
				channel.connect();
				System.out.println("Channel connected");
				
				while (true) {

					fileFound = doFileAndSchemeOperation(channel, identity);
					
					System.out.println("Channel disconnected");
					
					if (fileFound) {
						channel.disconnect();
						session.disconnect();
						return true;
					}

					Thread.sleep(2000);
	                //it runs until interval elapsed.
					if (System.currentTimeMillis() - starttime > interval) break;
				}
				
				channel.disconnect();
				session.disconnect();
				
			} catch (Exception ex) {
				System.out.println(ex);
			}
            //now kill the outer while
			if (System.currentTimeMillis() - starttime > interval) throw new Exception("Time elapsed. Application will close.");

            return fileFound;
        } catch (Exception e) {
            throw e;
        }
    }

    private boolean doFileAndSchemeOperation(final ChannelSftp channel,
            String identity) throws SftpException, UnsupportedEncodingException {
  
        Vector<ChannelSftp.LsEntry> dirList = channel.ls("/home/alice/M" + identity);
        String taskFolder = null;
		if (dirList.size() > 2)
		{
			for (int i = 0; i < dirList.size();i++)
			{
				taskFolder = dirList.get(i).getFilename();
				if (taskFolder.length() > 4) break;
				else taskFolder = null;
			}
		}
		if (taskFolder != null) {
			String fileloc = "/home/alice/M" + identity + "/" + taskFolder + "/GroupProjectM"
					+ identity + "Task";
			System.out.println(fileloc);
			channel.get(fileloc+".scm", MainJava.getFilesDir() + "\\GroupProjectM" + identity + "Task.scm");
			channel.get(fileloc+".scm.sign", MainJava.getFilesDir() + "\\GroupProjectM" + identity + "Task.scm.sign");
			channel.get(fileloc+".data", MainJava.getFilesDir() + "\\GroupProjectM" + identity + "Task.data");
			channel.rm(fileloc+".scm");
			channel.rm(fileloc+".scm.sign");
			channel.rm(fileloc+".data");
			channel.rmdir("/home/alice/M" + identity + "/" + taskFolder);
			System.out.println("File retrieved");
			return true;
		}
		return false;
  
    }
    
    protected void writeTo(String... args) {
        PublicKeyTarget target = new PublicKeyTarget(args[1], args[2], args[3]);
        String targetMachine = args[0]; // Identity of this PC
        String sourceMachine = args[4];
        try {
            final JSch jsch = new JSch();
            JSch.setConfig("StrictHostKeyChecking", "no");
            final Session session = target.getSession(jsch);
            session.connect();

             // SSH SFTP channel setup
            System.out.println("Starting channel");
            final ChannelSftp channel = (ChannelSftp) session
                    .openChannel("sftp");
            channel.connect();
            System.out.println("Channel connected");

            String srcFileLoc = MainJava.getFilesDir() + "\\GroupProjectM" + targetMachine + "Task";
            String dstFileLoc;
            System.out.println(srcFileLoc);
            String temptaskFolder;
            Vector<ChannelSftp.LsEntry> dirList = channel.ls("/home/alice/M" + targetMachine);
            if (dirList.size()==2)
            {
            	temptaskFolder = "/tempTask1";
            	dstFileLoc = "/home/alice/M" + targetMachine + temptaskFolder + "/GroupProjectM" + targetMachine + "Task";
                channel.mkdir("/home/alice/M" + targetMachine + temptaskFolder );            	
            }
            else
            {
            	temptaskFolder = "/tempTask" + (dirList.size()-1);
            	dstFileLoc = "/home/alice/M" + targetMachine + temptaskFolder + "/GroupProjectM" + targetMachine + "Task";
            	channel.mkdir("/home/alice/M" + targetMachine + temptaskFolder);
            }

            channel.put(srcFileLoc+".scm", dstFileLoc+".scm");
            channel.put(srcFileLoc+".scm.sign", dstFileLoc+".scm.sign");
            channel.put(srcFileLoc+".data", dstFileLoc+".data");
            String realTaskFoler = temptaskFolder.substring(5);
            channel.rename("/home/alice/M" + targetMachine + temptaskFolder, "/home/alice/M" + targetMachine + "/" + realTaskFoler);
            System.out.println("File(s) uploaded");

            // Complete and close the connection
            channel.disconnect();
            session.disconnect();
            System.out.println("Channel disconnected");
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

	public void setTimetorun(String timetoRun, long starttime) {
		if (timetoRun == null) interval = 999999999;
		else interval = Long.parseLong(timetoRun);
		this.starttime = starttime;
	}
}

/**
 * @author Team 5 SE 525 Software Security Architecture Final Project
 *         PublicKeyTarget
 */
class PublicKeyTarget {
    /*
     * Connection information; public key file stored in program's file
     * directory
     */
    private String hostname = "localhost";
    private String username = "alice";
    private int port = 22;
    String privateKeyFileName = "files\\se525hw3_rsa";

    PublicKeyTarget(String h, String u, String p) {
        hostname = h;
        username = u;
        port = Integer.parseInt(p);
    }

    Session getSession(JSch jsch) throws JSchException {
        System.out.format("Connecting to %s@%s:%d%n", username, hostname, port);
        byte[] passwordBytes1 = stringToBytesASCII(new String("alice")
                .toCharArray());
        jsch.addIdentity(privateKeyFileName, passwordBytes1);
        Session session = jsch.getSession(username, hostname, port);
        return session;
    }

    public byte[] stringToBytesASCII(char[] password) {
        byte[] b = new byte[password.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) password[i];
        }
        return b;
    }
}
