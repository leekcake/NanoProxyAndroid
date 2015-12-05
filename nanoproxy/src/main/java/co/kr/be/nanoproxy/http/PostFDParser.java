package co.kr.be.nanoproxy.http;

import java.io.InputStream;
import java.util.HashMap;

// Post Form Data Parser
public final class PostFDParser {
	protected final HashMap<String, String> Datas = new HashMap<>();

	public PostFDParser(final byte[] buffer) throws Exception {
		this(new String(buffer));
	}

	public PostFDParser(String post) throws Exception {
		post = post.trim();
		String[] split;
		if (post.contains("&")) {
			split = post.split("&");
		} else {
			split = new String[1];
			split[0] = post;
		}

		for (final String data : split) {
			// name and content
			if (data.endsWith("=")) {
				Datas.put(data.substring(0, data.length() - 1), "");
			} else {
				final String[] nc = data.split("=");
				Datas.put(nc[0], nc[1]);
			}
		}
	}

	public static final PostFDParser getFromRequest(final HeaderParser parser, final InputStream is) throws Exception {
		return getFromRequest(parser, is, 8192);
	}

	public static final PostFDParser getFromRequest(final HeaderParser parser, final InputStream is, final int Limit) throws Exception {
		final int size = Integer.parseInt(parser.getHeader("Content-Length"));
		// Bad Request
		if (size > Limit && Limit != -1)
			return null;
		else {
			final byte[] buffer = new byte[size];
			is.read(buffer);

			return new PostFDParser(buffer);
		}
	}

	public final String getFD(final String Name) {
		if (Datas.containsKey(Name))
			return Datas.get(Name);
		else
			return null;
	}
}
