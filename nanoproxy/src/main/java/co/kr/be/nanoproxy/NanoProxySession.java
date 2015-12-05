package co.kr.be.nanoproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.util.zip.GZIPInputStream;

import co.kr.be.nanoproxy.filter.FilterInformation;
import co.kr.be.nanoproxy.filter.SessionFilter;
import co.kr.be.nanoproxy.http.HeaderBuilder;
import co.kr.be.nanoproxy.http.HeaderParser;
import co.kr.be.nanoproxy.util.Util;

public final class NanoProxySession extends Thread {
	private final Socket socket;
	private final SessionFilter filter;
	private final FilterInformation filter_info = new FilterInformation();

	public NanoProxySession(final Socket socket, final SessionFilter filter) {
		this.socket = socket;
		this.filter = filter;
	}

	@Override
	public final void run() {
		try {
			final InputStream is = socket.getInputStream();
			// OutputStream os = pSocket.getOutputStream();
			int time = 15000;
			while (time != 0) {
				if (socket.isInputShutdown() == true || socket.isClosed()) {
					break;
				}
				if (is.available() > 100) {
					time = 15000;
					Handle();
				} else {
					sleep(10);
					time -= 10;
					if (time == 0) {
						break;
					}
				}
			}
			socket.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		try {
			socket.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public final void Handle() throws Exception {
		// HTTP Proxy Step!
		// 1. 클라이언트로 부터 요청 전문을 읽어옴
		// 1-1. 필요한경우 이 요청을 수정
		// 1-2. 필요한경우 요청을 서버에 전달하지 않고 프록시에서 처리
		// 2. 요청 전문을 요청받은 서버에 전달
		// 3. 서버로 부터 답변을 받아옴
		// 4. 답변을 서버로 재전송

		// 0. 관련 변수 초기화
		final BufferedInputStream localIs = new BufferedInputStream(socket.getInputStream());
		final BufferedOutputStream localOs = new BufferedOutputStream(socket.getOutputStream());
		filter_info.reset();

		// 1. 요청 전문 읽어 오기
		final HeaderParser parser = new HeaderParser(localIs, true);
		filter_info.setClientRequestHeader(parser);
		final HeaderBuilder builder = new HeaderBuilder();
		filter_info.setServerRequestHeader(builder);

		String encodedRequestURL = parser.getHeader("RequestOriginal");
		if (!encodedRequestURL.startsWith("http://")) {
			final StringBuffer buf = new StringBuffer();
			buf.append("http://");
			buf.append(encodedRequestURL);
			encodedRequestURL = buf.toString();
		}
		System.out.println("Received Request: " + encodedRequestURL);
		final URI uri = new URI(encodedRequestURL);
		filter_info.setRequestURL(uri);
		final String domain = uri.getHost();
		// domain = domain.startsWith("www.") ? domain.substring(4) : domain; // www.leekcake.com to leekcake.com
		int port = uri.getPort();
		if (port == -1) {
			port = 80;
		}

		// http://leekcake.com/ to /
		String localRequest = encodedRequestURL;
		localRequest = localRequest.substring(localRequest.indexOf(domain) + domain.length(), localRequest.length());
		builder.addHeader(parser.getHeader("Method").toUpperCase() + " " + localRequest + " " + parser.getHeader("HTTP Version"));
		parser.copyTo(builder);

		byte[] postData = null;
		if (parser.getHeader("Method").equals("post") && filter.needRequestPostData(filter_info)) {
			postData = new byte[Integer.parseInt(parser.getHeader("Content-Length"))];
			localIs.read(postData);
			filter_info.postData = postData;
			filter.onClientPostRequestwithPostData(filter_info);
		} else if (parser.getHeader("Method").equals("post")) {
			filter.onClientPostRequest(filter_info);
		} else {
			filter.onClientGetRequest(filter_info);
		}

		// 2. 요청받은 서버에 전달
		final Socket responseSocket = new Socket(domain, port);
		final BufferedOutputStream bos = new BufferedOutputStream(responseSocket.getOutputStream());
		bos.write(builder.getHeader());
		bos.flush();

		if (parser.getHeader("Method").equals("post")) {
			if (postData == null) {
				Util.copyStream(localIs, bos, Integer.parseInt(parser.getHeader("Content-Length")));
			} else {
				bos.write(postData);
			}
		}
		bos.flush();

		if (filter.needResponseOverride(filter_info)) {
			responseSocket.close();
			filter.onServerResponseOverride(filter_info, localOs);
			return;
		}

		// 3. 서버로 부터 답변을 받아옴
		final BufferedInputStream responseStream = new BufferedInputStream(responseSocket.getInputStream());
		final HeaderParser responseParser = new HeaderParser(responseStream, false);
		filter_info.setServerResponseHeader(responseParser);
		final HeaderBuilder responseBuilder = new HeaderBuilder();
		filter_info.setClientResponseHeader(responseBuilder);
		responseBuilder.addHeader(responseParser.getHeader("HTTP Version") + " " + responseParser.getHeader("ResponseCode") + " " + responseParser.getHeader("Message"));
		responseParser.copyTo(responseBuilder);

		filter.onServerResponse(filter_info);

		localOs.write(responseBuilder.getHeader());
		localOs.flush();

		// 3 + 4. 답변의 본문을 받아서 그대로 전달
		final BufferedOutputStream response;
		final ByteArrayOutputStream response_baos;
		final boolean needResponse = filter.needResponseBody(filter_info);
		if (needResponse) {
			response_baos = new ByteArrayOutputStream();
			response = new BufferedOutputStream(response_baos);
		} else {
			response = null;
			response_baos = null;
		}

		if (responseParser.getHeader("Content-Length") != null) {
			final int length = Integer.parseInt(responseParser.getHeader("Content-Length"));
			if (needResponse) {
				int left = length;
				int readed = 0;
				final byte[] buf = new byte[1024];
				while (left != 0) {
					readed = responseStream.read(buf, 0, Math.min(1024, left));
					if (readed == -1) {
						continue;
					}
					left -= readed;
					localOs.write(buf, 0, readed);
					response_baos.write(buf, 0, readed);
					localOs.flush();
					response_baos.flush();
				}
			} else {
				Util.copyStream(responseStream, localOs, length);
			}

		} else if (responseParser.getHeader("Transfer-Encoding") != null && responseParser.getHeader("Transfer-Encoding").equals("chunked")) {
			String length;
			int chunkLen;
			final byte[] buf = new byte[1024];
			while (true) {
				length = Util.readLine(responseStream);
				chunkLen = Integer.parseInt(length, 16); // \r\n

				localOs.write(length.getBytes());
				localOs.write("\r\n".getBytes());
				localOs.flush();

				if (chunkLen == 0) {
					localOs.write("\r\n".getBytes());
					break;
				}
				int left = chunkLen;
				int readed = 0;
				while (left != 0) {
					readed = responseStream.read(buf, 0, Math.min(1024, left));
					if (readed == -1) {
						continue;
					}
					left -= readed;
					if (needResponse) {
						response.write(buf, 0, readed);
					}
					localOs.write(buf, 0, readed);
					localOs.flush();
				}
				responseStream.read(buf, 0, 2);
			}
		} else if (responseParser.getHeader("ResponseCode").equals("403")) {
			// Do nothing
		} else {
			System.out.println("Not Happen?");
			int readed = 0;
			final byte[] buf = new byte[1024];
			while (responseStream.available() != 0) {
				readed = responseStream.read(buf, 0, 1024);
				if (needResponse) {
					response.write(buf, 0, readed);
				}
				localOs.write(buf, 0, readed);
				localOs.flush();
			}
		}
		try {
			localOs.flush();
		} catch (final IOException io) {
			io.printStackTrace();
		}
		responseSocket.close();

		if (needResponse) {
			response.flush();
			response_baos.flush();
			if (responseParser.getHeader("Content-Encoding") != null && responseParser.getHeader("Content-Encoding").equalsIgnoreCase("gzip")) {
				// System.out.println("GZIP Decompress!");
				final ByteArrayInputStream bais = new ByteArrayInputStream(response_baos.toByteArray());
				response_baos.reset();
				final GZIPInputStream gin = new GZIPInputStream(bais);
				Util.copyStream(gin, response_baos);
			}
			filter.onServerResponseBody(filter_info, response_baos.toByteArray());
		}

		System.out.println("Response Sent!");
		/*
		 * if (parser.getHeader("Method").equals("get")) { conn.setRequestMethod("GET"); } else if (parser.getHeader("Method").equals("post")) { conn.setDoOutput(true); conn.setRequestMethod("POST");
		 * HttpConnectionResult.copyStream(is, conn.getOutputStream(), Integer.parseInt(parser.getHeader("Content-Length"))); } else { os.write("".getBytes()); os.flush(); return; } final
		 * HttpConnectionResult result = new HttpConnectionResult(conn); result.flush(os); conn.disconnect();
		 */
	}
}
