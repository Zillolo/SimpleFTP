package net.tfobz.tele.eggale.ftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

	/**
	 * The default control port of the Server.
	 */
	public static final int CPORT = 1555;

	/**
	 * Maximum amount of concurrently active threads.
	 */
	public static final int MAX_THREADS = 5;
	
	/**
	 * The Servers greeting message.
	 */
	public static final String GREETING_MSG = "SimpleFTP ready. Enjoy.";
	
	/**
	 * The Servers default working directory.
	 */
	public static final String DEFAULT_DIR = "/srv/ftp/pub";

	private ServerSocket server;

	private ArrayList<Thread> threadPool;

	public Server(int port) {
		System.out.println("Initializing Server...");
		try {
			server = new ServerSocket(port);

			threadPool = new ArrayList<Thread>();
		} catch (IOException e) {
			System.err.println("[ERROR] Couldn't create Server Socket.");
		}
	}

	public void loop() {
		System.out.println("Server running.");
		while (true) {
			try {
				Socket client = server.accept();
				System.out.println("Client detected.");

				Iterator<Thread> iterator = threadPool.iterator();
				while (iterator.hasNext()) {
					Thread thread = iterator.next();
					if (thread.isAlive() == false) {
						iterator.remove();
						System.out.println("Thread " + thread.getId()
								+ " has been removed from the pool.");
					}
				}

				if (threadPool.size() < MAX_THREADS) {
					ControlThread thread = new ControlThread(client);
					threadPool.add(thread);
					System.out.println("Thread " + thread.getId()
							+ " has been added to the pool.");

					thread.start();
				} else {
					client.close();
					System.out.println("Connection refused. Thread pool full.");
				}
			} catch (IOException e) {
				System.err.println("[ERROR] Communication with Socket failed.");
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server(CPORT);

		server.loop();
	}
}
