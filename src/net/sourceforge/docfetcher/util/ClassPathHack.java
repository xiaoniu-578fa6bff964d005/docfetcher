package net.sourceforge.docfetcher.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import net.sourceforge.docfetcher.util.annotations.NotNull;

public final class ClassPathHack {

	private static final Class<?>[] parameters = new Class[] { URL.class };
	
	private ClassPathHack() {
	}

	public static void addFile(@NotNull String filename) throws IOException {
		addFile(new File(filename));
	}

	public static void addFile(@NotNull File file) throws IOException {
		addURL(file.toURI().toURL());
	}

	public static void addURL(@NotNull URL u) throws IOException {
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<?> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] { u });
		}
		catch (Throwable t) {
			throw new IOException(
				"Could not add URL to system classloader.");
		}
	}
}
