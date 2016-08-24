package com.cooksys.assessment.model;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class User {
	private String username;
	private Socket socket;
	static AtomicInteger i = new AtomicInteger(0);
	private int id;
	
	public User(String username, Socket socket) {
		this.username = username;
		this.socket = socket;
		this.id = i.getAndIncrement();
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}

}
