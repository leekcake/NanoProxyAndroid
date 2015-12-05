package co.kr.be.nanoproxy.http;

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;

import co.kr.be.nanoproxy.util.Util;

public final class HeaderParser {
	private final HashMap<String, String> Headers = new HashMap<String, String>();
	private final HashMap<String, String> Querys = new HashMap<String, String>();

	public HeaderParser(final InputStream is, final boolean request) throws Exception {
		final String front = Util.readLine(is);
		if (request) {
			parseRequestHeader(front);
		} else {
			parseResponseHeader(front);
		}

		String header;
		while (!(header = Util.readLine(is)).equals("")) {
			final int inx = header.indexOf(':');
			addHeader(header.substring(0, inx).trim(), header.substring(inx + 1, header.length()).trim());
		}
	}

	private final void parseRequestHeader(final String line) throws Exception {
		final String[] head = line.split(" ");
		if (head.length < 3)
			throw new Exception("Unknown HTTP Header: " + line);
		if (head[1].startsWith("/")) {
			head[1] = head[1].substring(1);
		}
		addHeader("Method", head[0].toLowerCase());
		if (head[1].contains("?")) {
			final int split = head[1].indexOf('?');
			addHeader("RequestURL", URLDecode(head[1].substring(0, split)));
			final String Addition = head[1].substring(split + 1);
			if (Addition.contains("&")) {
				final String[] additions = Addition.split("&");
				for (final String addition : additions) {
					final String[] info = addition.split("=");
					if (info.length == 2) {
						addQuery(info[0], info[1]);
					} else {
						addQuery(info[0], null);
					}
				}
			} else {
				final String[] info = Addition.split("=");
				if (info.length == 2) {
					addQuery(info[0], info[1]);
				} else {
					addQuery(info[0], null);
				}
			}
		} else {
			addHeader("RequestURL", URLDecode(head[1]));
		}
		addHeader("Request", URLDecode(head[1]));
		addHeader("RequestOriginal", head[1]);
		addHeader("HTTP Version", head[2]);
	}

	private final void parseResponseHeader(final String line) throws Exception {
		int sinx = 0, einx = 0;
		einx = line.indexOf(" ");
		addHeader("HTTP Version", line.substring(sinx, einx).trim());
		sinx = einx;
		einx = line.indexOf(" ", sinx + 1);
		addHeader("ResponseCode", line.substring(sinx, einx).trim());
		addHeader("Message", line.substring(einx + 1).trim());
	}

	private final String URLDecode(final String Value) throws Exception {
		return URLDecoder.decode(Value, "UTF-8");
	}

	private final void addHeader(final String Name, final String Content) {
		Headers.put(Name.toLowerCase(), Content);
	}

	private final void addQuery(final String Name, final String Content) {
		Querys.put(Name.toLowerCase(), Content);
	}

	public final String getHeader(final String Name) {
		if (Headers.containsKey(Name.toLowerCase()))
			return Headers.get(Name.toLowerCase());
		else
			return null;
	}

	public final boolean containsQuery(final String Name) {
		return Querys.containsKey(Name);
	}

	public final String getQuery(final String Name) {
		if (Querys.containsKey(Name.toLowerCase()))
			return Querys.get(Name.toLowerCase());
		else
			return null;
	}

	public final void copyTo(final HeaderBuilder builder) {
		for (final String key : Headers.keySet()) {
			if (key.equals("method") || key.equals("request") || key.equals("requestURL") || key.equals("http version") || key.equals("responsecode") || key.equals("message")) {
				continue;
			}

			builder.addHeader(key, Headers.get(key));
		}
	}
}
