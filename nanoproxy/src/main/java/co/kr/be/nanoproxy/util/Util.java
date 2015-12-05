package co.kr.be.nanoproxy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Util {
	public static final String readLine(final InputStream inputStream) throws IOException {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int c;
		for (c = inputStream.read(); c != '\n'; c = inputStream.read()) {
			if (c == '\r' || c == -1) {
				continue;
			}
			byteArrayOutputStream.write(c);
		}
		if (c == -1 && byteArrayOutputStream.size() == 0)
			return null;
		final String line = byteArrayOutputStream.toString("UTF-8");
		return line;
	}

	public final static void copyStream(final InputStream src, final OutputStream dest, final int count) throws IOException {
		int left = count;
		int readed = 0;
		final byte[] buf = new byte[1024];
		while (left != 0) {
			readed = src.read(buf, 0, Math.min(1024, left));
			if (readed == -1) {
				continue;
			}
			left -= readed;
			dest.write(buf, 0, readed);
			dest.flush();
		}
	}

	public final static void copyStream(final InputStream src, final OutputStream dest) throws IOException {
		int readed = 0;
		final byte[] buf = new byte[1024];
		while ((readed = src.read(buf)) != -1) {
			if (readed == -1) {
				continue;
			}
			dest.write(buf, 0, readed);
			dest.flush();
		}
		dest.flush();
	}
}
