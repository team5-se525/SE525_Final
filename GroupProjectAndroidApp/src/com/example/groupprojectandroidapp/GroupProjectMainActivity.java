package com.example.groupprojectandroidapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ExecutorService;

import jscheme.JScheme;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;



public class GroupProjectMainActivity extends Activity{

	private static String filesDir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);
            
            ScrollView sv = new ScrollView(this);
            sv.addView(ll);
            sv.setFillViewport(true);
            setContentView(sv);
            
            final TextView identityTV = new TextView(this);
            identityTV.setText("Enter Identity:");
            final EditText etIdentity = new EditText(this);
            etIdentity.setText("1");
            
            final TextView runtimeTV = new TextView(this);
            runtimeTV.setText("Enter Time to Run:");
            final EditText etTimetoRun = new EditText(this);
            etTimetoRun.setText("100000");
            
            final String host = "192.168.1.80";
            final String user = "alice";
            final String port = "22";
            
            final Button btnEnable = new Button(this);
            btnEnable.setText( "Start" );
           
                       
            // Actions upon clicking the enable button
            btnEnable.setOnClickListener( new View.OnClickListener() {
                    public void onClick( View v ) {
                		//Thread t = 
                		new Thread(new Runnable() {
                			public void run() {
                		        JschExec jsch = new JschExec();
                		        long starttime = System.currentTimeMillis();
                		        String timetoRun = etTimetoRun.getText().toString();
                		        String identity = etIdentity.getText().toString();
                		        filesDir = getFilesDir().getAbsolutePath() + "/m" + etIdentity.getText().toString();
                		        jsch.setTimetorun(timetoRun, starttime);
                		        try {
                					while (true) {
                						// it runs until timetorun elapsed.
                						if (jsch.connectandGetFile(identity, host, user, port))
                							runProgram(Integer.parseInt(identity));

                					}
                				} catch (Exception e) {
                					System.out.println("Error caught : " + e.toString());
                					return;
                				}
                			}
                		}).start();
                		//t.start();
                    }
            });
            
           
            // Add the views to the screen
            ll.addView( identityTV );
            ll.addView( etIdentity );
            ll.addView( runtimeTV );
            ll.addView( etTimetoRun );
            ll.addView( btnEnable );
    }

    public static String getlocalFileDir() {
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

	public void runProgram(int identity) {

        System.out.println("Inside Run Program");

        try {

            String filename;
            boolean isPresent = false;
            File f;

            filename = GroupProjectMainActivity.getlocalFileDir() + "/GroupProjectM" + identity + "Task.scm";
            f = new java.io.File(filename);

            // validate file
            isPresent = f.exists();
            String[] verify = { "-v", filename, filename + ".sign" };
            
            String datafilename = "/GroupProjectM" + identity + "Task.data";
            File datafile = new java.io.File(GroupProjectMainActivity.getlocalFileDir() + datafilename);
            RandomAccessFile raf = new RandomAccessFile(datafile, "r");
            String data = raf.readLine();
            String requester = data.substring(data.indexOf('=')+1);            
            
            SignVerifyFileDSA.alias = requester;
            //SignVerifyFileDSA.aliasPassword = ("se525team5"+requester).toCharArray();
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
                TaskGenerator tg = new GroupProjectMainActivity.TaskGenerator(identity);
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
    public static class CommonTextView {
        public static String getText() {
            return "";
        };

        public static void setText(String msg) {
            System.out.println(msg);
        }
    }

    public class TaskGenerator {
        int myId;
        public TaskGenerator(int i) {
            myId = i;
        }

        public void setText(String msg) {
            CommonTextView.setText(CommonTextView.getText() + "\n" + msg);
        }

        public void spawnTask(int identity) {
            CommonTextView.setText(CommonTextView.getText() + "\n"
                    + "Will spawn new task and copy to M2.");
            
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
    					+ "(.spawnTask tv 1)\n "
    					+ "(.setText tv \"Spawned Task.\"))";
                
                File newTargetFile = new File( GroupProjectMainActivity.getlocalFileDir() + "/GroupProjectM" + target + "Task.scm");
                fw = new FileWriter( newTargetFile );
                fw.write( content );
                fw.close();
                
                content = "Source=m" + source;
                File newTargetFileData = new File(GroupProjectMainActivity.getlocalFileDir() + "/GroupProjectM" + target + "Task.data");
                fw = new FileWriter( newTargetFileData );
                fw.write( content );
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error caught : " + e.toString());
            }
            
            
            String filename = GroupProjectMainActivity.getlocalFileDir() + "/GroupProjectM" + target + "Task.scm";
            String[] sign = { "-s", filename, filename + ".sign" };
            try {
                SignVerifyFileDSA.alias = "m" + source;
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
            
            File newlocalTargetFile = new File( "GroupProjectM" + target + "Task.scm");
            File newlocalSignedTargetFile = new File( "GroupProjectM" + target + "Task.scm.sign");
            File newlocalTargetDataFile = new File( "GroupProjectM" + target + "Task.data");
            newlocalTargetFile.delete();
            newlocalSignedTargetFile.delete();
            newlocalTargetDataFile.delete();
        }
    }

}

