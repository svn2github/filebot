
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.gio.GVFS;


public class FileTransferable implements Transferable {
	
	public static final DataFlavor uriListFlavor = createUriListFlavor();
	
	
	private static DataFlavor createUriListFlavor() {
		try {
			return new DataFlavor("text/uri-list;class=java.nio.CharBuffer");
		} catch (ClassNotFoundException e) {
			// will never happen
			throw new RuntimeException(e);
		}
	}
	
	private final File[] files;
	
	
	public FileTransferable(File... files) {
		this.files = files;
	}
	
	
	public FileTransferable(Collection<File> files) {
		this.files = files.toArray(new File[0]);
	}
	
	
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.isFlavorJavaFileListType())
			return Arrays.asList(files);
		else if (flavor.equals(uriListFlavor))
			return CharBuffer.wrap(getUriList());
		else
			throw new UnsupportedFlavorException(flavor);
	}
	
	
	/**
	 * @return line separated list of file URIs
	 */
	private String getUriList() {
		StringBuilder sb = new StringBuilder(80 * files.length);
		
		for (File file : files) {
			// use URI encoded path
			sb.append("file://").append(file.toURI().getRawPath());
			sb.append("\r\n");
		}
		
		return sb.toString();
	}
	
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.javaFileListFlavor, uriListFlavor };
	}
	
	
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return isFileListFlavor(flavor);
	}
	
	
	public static boolean isFileListFlavor(DataFlavor flavor) {
		return flavor.isFlavorJavaFileListType() || flavor.equals(uriListFlavor);
	}
	
	
	public static boolean hasFileListFlavor(Transferable tr) {
		return tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || tr.isDataFlavorSupported(FileTransferable.uriListFlavor);
	}
	
	
	@SuppressWarnings("unchecked")
	public static List<File> getFilesFromTransferable(Transferable tr) throws IOException, UnsupportedFlavorException {
		if (tr.isDataFlavorSupported(FileTransferable.uriListFlavor)) {
			// file URI list flavor (Linux)
			Readable transferData = (Readable) tr.getTransferData(FileTransferable.uriListFlavor);
			Scanner scanner = new Scanner(transferData);
			List<File> files = new ArrayList<File>();
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				if (line.startsWith("#")) {
					// the line is a comment (as per RFC 2483)
					continue;
				}
				
				try {
					URI uri = new URI(line);
					File file = null;
					
					try {
						// file URIs
						file = new File(uri);
					} catch (IllegalArgumentException e) {
						// try handle other GVFS URI schemes
						try {
							if (GVFS.isSupported()) {
								file = GVFS.getPathForURI(uri);
							}
						} catch (LinkageError error) {
							Logger.getLogger(FileTransferable.class.getName()).log(Level.WARNING, "Unable to resolve GVFS URI", e);
						}
					}
					
					if (file == null || !file.exists()) {
						throw new FileNotFoundException(line);
					}
					
					files.add(file);
				} catch (Throwable e) {
					// URISyntaxException, IllegalArgumentException, FileNotFoundException, LinkageError, etc
					Logger.getLogger(FileTransferable.class.getName()).log(Level.WARNING, "Invalid file URI: " + line);
				}
				return files;
			}
		} else if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			// file list flavor
			return (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
		}
		
		// cannot get files from transferable
		throw new UnsupportedFlavorException(null);
	}
}
