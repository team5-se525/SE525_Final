package com.example.groupprojectandroidapp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;

public class SignVerifyFileDSA {
    static String filename = GroupProjectMainActivity.getlocalFileDir().substring(0, GroupProjectMainActivity.getlocalFileDir().length()-2) + "/team5ks-PKCS12.p12";
    static String alias = "m2";
    final static char[] keyStorePassword = "se525team5".toCharArray();
    static char[] aliasPassword = "se525team5m2".toCharArray();
    final static String algorithmName = "SHA1withDSA";

    static void usage() {
        System.err.println("Usage: java SignVerifyFileRSA (-s | -v) <file-to-process> <signature-file>");
        System.exit(1);
    }

    
    public static boolean verify( String[] args ) throws 
        GeneralSecurityException, IOException {
        
        if (args.length != 3) {
            usage();
        }

        String inputFilename = args[1];
        String signatureFilename = args[2];

        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(filename);
        ks.load(fis, keyStorePassword);
        fis.close();

        Signature signature = Signature.getInstance(algorithmName);

        Certificate cert = (Certificate) ks.getCertificate(alias);
        PublicKey pubKey = cert.getPublicKey();
        signature.initVerify(pubKey); // Could also use cert directly.

        RandomAccessFile raf = new RandomAccessFile(inputFilename, "r");
        byte[] inputText = new byte[(int) raf.length()];
        raf.readFully(inputText);
        raf.close();

        signature.update(inputText);

        // Read signatureData from signatureFilename.
        raf = new RandomAccessFile(signatureFilename, "r");
        byte[] signatureData = new byte[(int) raf.length()];
        raf.readFully(signatureData);
        raf.close();
        
        // Check that signatureData matches the value calculated for the
        // real data.
        if (signature.verify(signatureData)) {
            System.out.println("Signature verified successfully");
            return true;
        } else {
            System.err.println("Signature NOT verified successfully");
            return false;
        }
    } // verify method
    
    public static void sign(String[] args) throws 
        GeneralSecurityException, IOException {
        
        if (args.length != 3) {
            usage();
        }

        String inputFilename = args[1];
        String signatureFilename = args[2];

        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(filename);
        ks.load(fis, keyStorePassword);
        fis.close();

        Signature signature = Signature.getInstance(algorithmName);
        PrivateKey privKey = (PrivateKey) ks.getKey(alias, aliasPassword);
        signature.initSign(privKey);

        RandomAccessFile raf = new RandomAccessFile(inputFilename, "r");
        byte[] inputText = new byte[(int) raf.length()];
        raf.readFully(inputText);
        raf.close();

        signature.update(inputText);
        
        byte[] signatureData = signature.sign();
        // Write signatureData to signatureFilename.
        FileOutputStream outputFos = new FileOutputStream(signatureFilename);
        outputFos.write(signatureData);
        outputFos.close();

    }
    
    public static void main(String[] args) throws GeneralSecurityException,
            IOException {
        if (args.length != 3) {
            usage();
        }

        boolean sign = true;
        if (args[0].equals("-s")) {
            sign = true;
        } else if (args[0].equals("-v")) {
            sign = false;
        } else {
            usage();
        }

        String inputFilename = args[1];
        String signatureFilename = args[2];

        KeyStore ks = KeyStore.getInstance("JCEKS");
        FileInputStream fis = new FileInputStream(filename);
        ks.load(fis, keyStorePassword);
        fis.close();

        Signature signature = Signature.getInstance(algorithmName);

        if (sign) {
            PrivateKey privKey = (PrivateKey) ks.getKey(alias, aliasPassword);
            signature.initSign(privKey);
        } else {
            Certificate cert = (Certificate) ks.getCertificate(alias);
            PublicKey pubKey = cert.getPublicKey();
            signature.initVerify(pubKey); // Could also use cert directly.
        }

        RandomAccessFile raf = new RandomAccessFile(inputFilename, "r");
        byte[] inputText = new byte[(int) raf.length()];
        raf.readFully(inputText);
        raf.close();

        signature.update(inputText);

        if (sign) {
            byte[] signatureData = signature.sign();
            // Write signatureData to signatureFilename.
            FileOutputStream outputFos = new FileOutputStream(signatureFilename);
            outputFos.write(signatureData);
            outputFos.close();

        } else {
            // Read signatureData from signatureFilename.
            raf = new RandomAccessFile(signatureFilename, "r");
            byte[] signatureData = new byte[(int) raf.length()];
            raf.readFully(signatureData);
            raf.close();
            // Check that signatureData matches the value calculated for the
            // real data.
            if (signature.verify(signatureData)) {
                System.out.println("Signature verified successfully");
            } else {
                System.err.println("Signature NOT verified successfully");
                System.exit(1);
            }
        }//end outer if/else
    }//end main
}//end class
