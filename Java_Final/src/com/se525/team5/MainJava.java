package com.se525.team5;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.util.Scanner;

import javax.swing.filechooser.FileSystemView;

import jscheme.JScheme;

public class MainJava {

    private static TextView mainOutput;
    private static String filesDir;
    /**
     * @param args
     */
    public static void main(String[] args) {

        // Retrieve the identity from the command line
/*        if (args.length < 1) {
            System.out.println("Usage requires 1 arguments\n");
            System.out.println("java MainJava.java <identity>");
            System.exit(0);
        }*/
        final String identity = "2";//args[0];
        final String host = "192.168.1.80";
        final String user = "alice";
        final String port = "22";
        final String timetoRun = "999999";//args[1];

/*        String filename = "files//GroupProjectM2Task.scm";
        String[] sign = { "-s", filename, filename + ".sign" };
        try {
            SignVerifyFileDSA.alias = "m1";
            SignVerifyFileDSA.aliasPassword = ("se525team5"+"m1").toCharArray();
            SignVerifyFileDSA.sign(sign);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        

        // Run a 'local thread' to check for new files pulled back
        filesDir = System.getProperty("user.dir") + "\\m" + identity;
		new Thread(new Runnable() {
			public void run() {
		        JschExec jsch = new JschExec();
		        long starttime = System.currentTimeMillis();
		        jsch.setTimetorun(timetoRun, starttime);
		        try {
					while (true) {
						// it runs until timetorun elapsed.
						if (jsch.connectandGetFile(identity, host, user, port))
							runProgram(Integer.parseInt(identity));

					}
				} catch (Exception e) {
					System.out.println(e.toString());
					return;
				}
			}
		}).start();
		
    }

    public static String getFilesDir()
    {
    	File f = new File(filesDir);
    	if (f.exists())	return filesDir;
    	else
    	{
    		try {
				f.mkdir();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error caught : " + e.toString());
			}
    		return filesDir;
    	}
    }
    
    public static void runProgram(int identity) {

        System.out.println("Inside Run Program");

        try {

            String filename;
            boolean isPresent = false;
            File f;

            filename = MainJava.getFilesDir() + "\\GroupProjectM" + identity + "Task.scm";
            f = new java.io.File(filename);

            // validate file
            isPresent = f.exists();
            String[] verify = { "-v", filename, filename + ".sign" };
            
            String datafilename = MainJava.getFilesDir() + "\\GroupProjectM" + identity + "Task.data";
            File datafile = new java.io.File(datafilename);
            RandomAccessFile raf = new RandomAccessFile(datafile, "r");
            String data = raf.readLine();
            String requester = data.substring(data.indexOf('=')+1);            
            
            SignVerifyFileDSA.alias = requester;
            SignVerifyFileDSA.aliasPassword = ("se525team5"+requester).toCharArray();
            boolean validSignature = SignVerifyFileDSA.verify(verify);
            
            if (isPresent && validSignature ) {

                final JScheme js = new JScheme();
                System.out.println("File is there");
                System.out.println("Signature is valid? " + validSignature );

                // Read the string out so the file can be closed and
                // deleted
                Scanner scan = new Scanner(f);
                String content = scan.useDelimiter("\\Z").next();
                scan.close();

                // Load the JScheme object and run 'main'
                // Object result = js.load( new FileReader(f) );
                Object result = js.load(content);
                System.out.println("Task2 " + result.toString());
                TaskGenerator tg = new TaskGenerator(mainOutput, identity);
                System.out.println("Preparing to run JScheme ");
                js.call("main", tg);
                tg.setText("Completed running JScheme");

                // boolean isDeleted = f.delete();
                System.out.println("File Was Deleted: " + f.delete());
             } 

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error caught : " + e.toString());
        }
    }

    // Class to take the place of the Android 'TextView'
    public static class TextView {
        public static String getText() {
            return "";
        };

        public static void setText(String msg) {
            System.out.println(msg);
        }
    }

    public static class TaskGenerator {
        int myId;
        public TaskGenerator(TextView mv, int i) {
            myId = i;
        }

        public void setText(String msg) {
            TextView.setText(TextView.getText() + "\n" + msg);
        }

        public void spawnTask(int identity) {
            TextView.setText(TextView.getText() + "\n"
                    + "Will spawn new task and copy to M1.");
            
            String target = Integer.toString(identity);
            String host = "192.168.1.80";
            String user = "alice";
            String port = "22";
            String source = "" + myId;
            
            FileWriter fw;
            Scanner scan;
            try {         	
				String content = "(define (main tv)\n "
						+ "(.setText tv \"This is Group Project Task1 via SSH.\")\n "
						+ "(.spawnTask tv 2)\n "
						+ "(.setText tv \"Spawned Task.\"))";
                
                File newTargetFile = new File( MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.scm");
                fw = new FileWriter( newTargetFile );
                fw.write( content );
                fw.close();
                
                content = "Source=m"+source;
                File newTargetFileData = new File( MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.data");
                fw = new FileWriter( newTargetFileData );
                fw.write( content );
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error caught : " + e.toString());
            }
            
            
            String filename = MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.scm";
            String[] sign = { "-s", filename, filename + ".sign" };
            try {
                SignVerifyFileDSA.alias = "m"+source;
                SignVerifyFileDSA.aliasPassword = ("se525team5"+"m"+source).toCharArray();
                SignVerifyFileDSA.sign(sign);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                System.out.println("Error caught : " + e.toString());
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error caught : " + e.toString());
            }
            
            JschExec jsch = new JschExec();
            jsch.writeTo(target, host, user, port, source, filename, filename+".sign");
            
            File newlocalTargetFile = new File( MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.scm");
            File newlocalSignedTargetFile = new File(MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.scm.sign");
            File newlocalTargetDataFile = new File( MainJava.getFilesDir() + "\\GroupProjectM" + target + "Task.data");
            newlocalTargetFile.delete();
            newlocalSignedTargetFile.delete();
            newlocalTargetDataFile.delete();
        }
    }
}
