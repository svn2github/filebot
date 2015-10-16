package net.filebot.archive;

import java.util.logging.Logger;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;

import com.sun.jna.Platform;

public class SevenZipLoader {

	private static boolean nativeLibrariesLoaded = false;

	private static synchronized void requireNativeLibraries() throws SevenZipNativeInitializationException {
		if (nativeLibrariesLoaded) {
			return;
		}

		// initialize 7z-JBinding native libs
		try {
			try {
				if (Platform.isWindows() && Platform.is64Bit()) {
					System.loadLibrary("libgcc_s_seh-1");
				}
			} catch (Throwable e) {
				Logger.getLogger(SevenZipLoader.class.getName()).warning("Failed to preload library: " + e);
			}

			System.loadLibrary("7-Zip-JBinding");
			SevenZip.initLoadedLibraries(); // NATIVE LIBS MUST BE LOADED WITH SYSTEM CLASSLOADER
			nativeLibrariesLoaded = true;
		} catch (Throwable e) {
			throw new SevenZipNativeInitializationException("Failed to load 7z-JBinding: " + e.getMessage(), e);
		}
	}

	public static IInArchive open(IInStream stream, IArchiveOpenCallback callback) throws Exception {
		// initialize 7-Zip-JBinding
		requireNativeLibraries();

		if (callback == null) {
			return SevenZip.openInArchive(null, stream);
		} else {
			return SevenZip.openInArchive(null, stream, callback);
		}
	}

}
