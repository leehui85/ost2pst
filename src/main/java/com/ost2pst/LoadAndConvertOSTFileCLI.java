package com.ost2pst;

/* https://github.com/aspose-email/Aspose.Email-for-Java */
import com.aspose.email.FileFormat;
import com.aspose.email.FolderInfo;
import com.aspose.email.FolderInfoCollection;
import com.aspose.email.PersonalStorage;

//import com.aspose.email.examples.Utils;
import java.io.File;
import java.io.RandomAccessFile;

public class LoadAndConvertOSTFileCLI {
	
	public static void showUsage(int rc) {
		System.out.println("USAGE: ost2pst.jar <input.ost> <output.pst>");
		System.exit(rc);
	}
	
	public static void main(String[] args) {		
		String inputFile = "";
		String outputFile = "";
		boolean debug = false;
		// Print version from pom.xml
		String sVer = String.format("OST2PST (%s)\n", Version.VERSION); 
		System.out.println("");
		System.out.println(sVer);
		// Handle arguments
		if (args.length == 0) {
			showUsage(0);
		} else {
			try { inputFile = args[0]; }
			catch(Exception e) {
				System.out.println("ERROR: no input file specified");
				showUsage(2);
			}
			try { outputFile = args[1]; }
			catch(Exception e) {
				System.out.println("ERROR: no output file specified");
				showUsage(2);
			}
			if (debug) {
 				System.out.println("DEBUG: args.length=" + args.length + " args[0]=" + args[0] + " args[1]=" + args[1]);
 			}
		}
		File fIn = new File(inputFile);	
		File fOut = new File(outputFile);
		if (debug) {
			System.out.println("DEBUG: fIn.exists=" + fIn.exists() + " fIn.isFile=" + fIn.isFile());
		}	
		// Check if input and output file exist
		if ( !(fIn.exists() && fIn.isFile()) ) {
			String sInErr = String.format("ERROR: input file \"%s\" does not exist", inputFile);
			System.out.println(sInErr);
			System.exit(2);
		}
		if (fIn.length() == 0) {
			String sInErr = String.format("ERROR: input file \"%s\" is empty", inputFile);
			System.out.println(sInErr);
			System.exit(2);
		}
		if ( fOut.exists() || fOut.isFile() ) {
			String sOutErr = String.format("ERROR: output file \"%s\" already exists", outputFile);
			System.out.println(sOutErr);
			System.exit(2);
		}

		//Read an OST file
		readAnOSTFile(debug, inputFile);
		//Converting OST to PST
		convertOSTToPST(debug, inputFile, outputFile);
	}

	public static void readAnOSTFile(final boolean debug, String inputFile) {
		// Load the Outlook PST file
		File fIn = new File(inputFile);
		PersonalStorage pst = null;
		FolderInfoCollection folderInfoCollection = null;
		String sLoad = String.format("INFO: Loading OST file \"%s\" (%s)", inputFile, humanReadableByteCount(fIn.length(), true));
		System.out.println(sLoad);
		try {
			pst = PersonalStorage.fromFile(inputFile);
		} catch (Exception e) {
			System.out.printf("ERROR: %s", e);
		}
		folderInfoCollection = pst.getRootFolder().getSubFolders();
		// Loop over all the-sub folders
		for (int i = 0; i < folderInfoCollection.size(); i++) {
			// Display all the folders
			FolderInfo folderInfo = folderInfoCollection.get_Item(i);
			System.out.printf("%5s Folder [%02d] \"%s\"\n", " ", i, folderInfo.getDisplayName());
		}
		System.out.println("");
	}

	// Get data version (NDB version) from offset 10
	public static int typeOSTFile(final boolean debug, String inputFile) {
		RandomAccessFile raf = null;
		final byte[] fileTypeBytes = new byte[2];
		try {
            raf = new RandomAccessFile(inputFile, "r");
			raf.seek(10);
			raf.readFully(fileTypeBytes);
		} catch (Exception e) {
			System.out.printf("ERROR: %s", e);
		}
		finally {
			try {
				raf.close();
			}
			catch (Exception e){
				System.out.printf("ERROR: %s", e);
			}
		}
		return (int) fileTypeBytes[0];
	}

	public static void convertOSTToPST(final boolean debug, final String inputFile, final String outputFile) {
		// Get file format type: array index is 'wVer' and maps to description String 
		int fmtTypeNum = typeOSTFile(debug, inputFile);
		String[] arr = new String[50];
		String fmtTypeStr = "Unknown";
		arr[14] = "32-bit ANSI";
		arr[15] = "32-bit ANSI";
		arr[21] = "64-bit Unicode, Visual Recovery";
		arr[23] = "64-bit Unicode";
		arr[36] = "64-bit Unicode 4k, Outlook 2013";
		if (arr[fmtTypeNum] != null) {
			fmtTypeStr = arr[fmtTypeNum];
		}
		System.out.printf("INFO: File format is \"%s\" (%d)\n", fmtTypeStr, fmtTypeNum);
		if (fmtTypeNum != 36) {
			// Prepare task to convert and save
	        Runnable r = new Runnable() {
	            public void run() {
	            	PersonalStorage ost = PersonalStorage.fromFile(inputFile);
	            	try {
	            		ost.saveAs(outputFile, FileFormat.Pst);
	            	} catch (com.aspose.email.system.exceptions.NotImplementedException e) {
	            		System.out.println("");
	            		System.out.printf("ERROR: unable to save file, make sure input file format is supported\n");
	            		System.out.printf("ERROR: %s", e);
	            	} catch (Exception e) {
	            		System.out.println("");
	            		System.out.printf("ERROR: %s", e);
	            	}
	            }
	        };
	        // Start thread and task
	        new Thread(r).start();
			File fIn = new File(inputFile);
			File fOut = new File(outputFile);
			// Show progress
			String spin = "----\\\\\\\\||||////";  // ".oO@"
			int i = 0;
			System.out.printf("INFO: Converting \"%s\" to \"%s\"\n", inputFile, outputFile);
			while (fIn.length() > fOut.length()) {
				if (debug) {
					System.out.println("DEBUG: fIn.length=" + fIn.length() + " fOut.length=" + fOut.length());
				}
				if ((int)Math.round((100.0 * fOut.length() / fIn.length())) >= 99) {
					spin = String.format("%16s", " ");
				}
				String sConv = String.format("%5s %dMB/%dMB (%d%%) %s", " ",
					(int)(fOut.length() / 1024 / 1024),
					(int)(fIn.length() / 1024 / 1024),
					(int)Math.round((100.0 * fOut.length() / fIn.length())),
					spin.charAt(i % 16)
				);
				int sLen = (int)(80 - sConv.length());
				if (sLen != 0) {
					String sPad = String.format("%" + sLen + "s", " ");
					System.out.print(sConv + sPad + "\r");
				}
	            i++;
			}
			System.out.println("");
		} else {
			System.out.printf("INFO: Saving Outlook 2013/2016 files is not supported, exiting...\n");
			System.exit(1);
		}
	}
	
	/* http://programming.guide/java/formatting-byte-size-to-human-readable-format.html */
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + "B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.0f%sB", bytes / Math.pow(unit, exp), pre);
	}	  
}