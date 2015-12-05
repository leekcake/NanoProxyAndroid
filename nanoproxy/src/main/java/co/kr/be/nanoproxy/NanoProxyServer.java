package co.kr.be.nanoproxy;

import java.io.IOException;
import java.net.ServerSocket;

import co.kr.be.nanoproxy.filter.SessionFilter;

public final class NanoProxyServer {
	private Worker worker = null;
	private SessionFilter filter = null;

	public NanoProxyServer() {

	}

	public final void Listen(final int port) throws IOException {
		worker = new Worker(port);
		worker.start();
	}

	public final void registerFilter(final SessionFilter filter) {
		this.filter = filter;
	}

	public final void stopListen() {
		while (worker.isAlive()) {
			worker.interrupt();
		}
	}

	private final class Worker extends Thread {
		public final ServerSocket socket;

		public Worker(final int port) throws IOException {
			socket = new ServerSocket(port);
		}

		@Override
		public final void run() {
			while (!isInterrupted()) {
				try {
					new NanoProxySession(socket.accept(), filter).start();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
