/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import net.tfobz.tele.eggale.ftp.state.AuthState;
import net.tfobz.tele.eggale.ftp.state.CState;

/**
 * @author alex
 * 
 */
public class ControlThread extends Thread {

	/**
	 * The client this Thread is handling.
	 */
	private Socket client;

	/**
	 * The control connection input.
	 */
	private BufferedReader cInput;

	/**
	 * The control connection output.
	 */
	private PrintWriter cOutput;

	private CState state = CState.GREETING;
	private AuthState authState = AuthState.USER;

	public ControlThread(Socket client) {
		System.out.println("Thread initializing...");
		this.client = client;

		try {
			cInput = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			cOutput = new PrintWriter(client.getOutputStream());
		} catch (IOException e) {
			System.err.println("Communication with Socket failed.");
		}
	}

	public void run() {
		while (Thread.currentThread().isInterrupted() == false) {
			try {
				switch (state) {
				case GREETING:
					reply(220, Server.GREETING_MSG);

					state = CState.AUTHENTICATION;
					break;
				case AUTHENTICATION:
					String command = cInput.readLine();

					switch (authState) {
					case USER:
						if (command.matches("USER .+")) {
							String user = command.substring(5);

							if (user.equals("anonymous")
									|| user.equals("Anonymous")) {
								reply(331, "Username ok. Password needed.");

								authState = AuthState.PASS;
							} else {
								reply(430, "Invalid username or password.");
							}
						} else {
							reply(332, "Need account for login.");
						}
						break;
					case PASS:
						if (command.matches("PASS .*")) {
							String password = command.substring(5);
							
						/*	if(user.equals("")) {
								reply(230, "Logged in. Proceed.");
								
								state = AuthState.ACCT;
							} */
						}
						break;
					}
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void reply(int code, String message) {
		cOutput.write(code + " " + message + "\r\n");
		cOutput.flush();
	}
}
