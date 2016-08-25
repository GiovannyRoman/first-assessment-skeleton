package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cooksys.assessment.model.Message;
import com.cooksys.assessment.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	static Set<User> users = new HashSet<User>();
	private Socket socket;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
				case "connect":  // O(n*n)
					
					boolean doubleUser = false;  //check if duplicate username
					for (User body : users) {
						if (body.getUsername().equals(message.getUsername())) {
							doubleUser = true;
							this.socket.close();
						}
					}
					if (doubleUser == true) {
						break;
					}

					String connect = LocalDateTime.now() + " user <" + message.getUsername() + ">  has connected";
					users.add(new User(message.getUsername(), socket));
					message.setContents(connect);
					log.info(connect);
					for (User everyone : users) {
						PrintWriter receiver = new PrintWriter(
								new OutputStreamWriter(everyone.getSocket().getOutputStream()));
						receiver.write(mapper.writeValueAsString(message));
						receiver.flush();
					}
					break;

				case "disconnect": // O(n * n)
					String disconnect = LocalDateTime.now() + " user <" + message.getUsername() + ">  has disconnected";
					message.setContents(disconnect);

					for (User everyone : users) {
						PrintWriter receiver = new PrintWriter(
								new OutputStreamWriter(everyone.getSocket().getOutputStream()));
						receiver.write(mapper.writeValueAsString(message));
						receiver.flush();
						
						if (everyone.getUsername().equals(message.getUsername())) {
							if (users.remove(everyone)) {
								everyone.getSocket().close();
							}
						}
					}

					log.info(disconnect);
					synchronized (this) {
						for (User everyone : users) {
							if (everyone.getUsername().equals(message.getUsername())) {
								if (users.remove(everyone)) {
									everyone.getSocket().close();
								}
							}
						}
						break;
					}
				case "echo": // O(1)
					String echoMessage = LocalDateTime.now() + " user <" + message.getUsername() + "> echoed message: "
							+ message.getContents();
					log.info(echoMessage);
					message.setContents(echoMessage);
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
					break;

				case "broadcast": // O(n)
					String broadmess = LocalDateTime.now() + " user <" + message.getUsername() + "> broadcast (all): "
							+ message.getContents();
					message.setContents(broadmess);
					log.info(broadmess);
					for (User everyone : users) {
						PrintWriter receiver = new PrintWriter(
								new OutputStreamWriter(everyone.getSocket().getOutputStream()));
						receiver.write(mapper.writeValueAsString(message));
						receiver.flush();
					}
					break;

				case "users": // O(n)
					String info = LocalDateTime.now() + " currently connected users: ";
					for (User user : users) {
						info += "\n" + user.getUsername();
					}
					log.info(info);
					message.setContents(info);
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
					break;

				default:

					if (message.getCommand().contains("@")) { // O(n)
						String buddy = message.getCommand().substring(1, message.getCommand().length());

						boolean hasFriend = false;
						for (User us : users) {
							if (us.getUsername().equals(buddy)) {
								String privateMessage = LocalDateTime.now() + " <" + message.getUsername()
										+ "> (whisper): " + message.getContents();
								log.info(privateMessage);
								hasFriend = true;

								message.setContents(privateMessage);
								String pmessage = mapper.writeValueAsString(message);
								PrintWriter receiver = new PrintWriter(
										new OutputStreamWriter(us.getSocket().getOutputStream()));
								receiver.write(pmessage);
								receiver.flush();
							}
						}
						if (!hasFriend) {
							String notFound = "User was not found";
							log.info(notFound);
							message.setContents(notFound);
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						break;
					} else {
						String err = "This is an invalid command.";
						log.info(err);
						message.setContents(err);
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						break;
					}
				}
			}
		} catch (IOException | ConcurrentModificationException e) {
			log.error("Something went wrong :/", e);
		}
	}
}
