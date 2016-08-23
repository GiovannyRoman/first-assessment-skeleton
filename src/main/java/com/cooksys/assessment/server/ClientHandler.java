package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.cooksys.assessment.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	static HashSet<User> users = new HashSet<User>(); // store all users
	private Socket socket;
	private String lastCommand;

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

				if (message.getCommand().contains("@")) {
					String buddy = message.getCommand().substring(1);
					for (User us : users) {
						if (us.getUsername() == buddy) {
							String privateMessage = "<"+message.getUsername()+"> (whisper): " + message.getContents();
							log.info(privateMessage);
							
							message.setContents(privateMessage);
							String pmessage = mapper.writeValueAsString(message);
							
							PrintWriter receiver = new PrintWriter(
									new OutputStreamWriter(us.getSocket().getOutputStream()));
							receiver.write(pmessage);
							receiver.flush();
							break;
						}
					}
				} else {
					switch (message.getCommand()) {
					case "connect":

						String connect = "user <" + message.getUsername() + ">  has connected";
						users.add(new User(message.getUsername(), socket));
						message.setContents(connect);
						log.info("user <{}> has connected", message.getUsername());

						String bmessage = mapper.writeValueAsString(message);
						for (User everyone : users) {
							PrintWriter receiver = new PrintWriter(
									new OutputStreamWriter(everyone.getSocket().getOutputStream()));
							receiver.write(bmessage);
							receiver.flush();
						}

						break;
						
					case "disconnect":
						String disconnect = "user <" + message.getUsername() + ">  has disconnected";
						
						message.setContents(disconnect);
						String dismessage = mapper.writeValueAsString(message);

						log.info(disconnect); 
						for (User everyone : users) {
							PrintWriter receiver = new PrintWriter(
									new OutputStreamWriter(everyone.getSocket().getOutputStream()));
							receiver.write(dismessage);
							receiver.flush();
							if(everyone.getUsername().equals( message.getUsername())){
								users.remove(everyone);
								everyone.getSocket().close();
							}
						}
						break;
						
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
						
					case "broadcast": 

						String broadmess = "user<"+message.getUsername()+"> broadcast (all) <"+message.getContents()+">";
						message.setContents(broadmess);
						String broadmessage = mapper.writeValueAsString(message);
						log.info(broadmess);
						for (User everyone : users) {
							PrintWriter receiver = new PrintWriter(
									new OutputStreamWriter(everyone.getSocket().getOutputStream()));
							receiver.write(broadmessage);
							receiver.flush();
						}
						break;
						
					case "users":
						String info = "currently connected users: ";
						for (User user : users) {
							info += "\n" + user.getUsername();
						}
						log.info(info);
						message.setContents(info);
						String newMessage = mapper.writeValueAsString(message);
						writer.write(newMessage);
						writer.flush();
						break;
					}

				}
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
}
