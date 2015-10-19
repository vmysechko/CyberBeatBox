package ua.com.mysechko.cyberbeatbox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * In this class we use ObjectInput/OutputStream classes because we work with serialized objects,
 * despite that first of it is a string!
 */
public class CyberBeatBoxServer {

	ArrayList <ObjectOutputStream> clientsMessages;
	
	/*Class to handle the clients income inquiries*/
	public class ClientHandler implements Runnable{
		ObjectInputStream oit;
		Socket clientSocket;
		
		public ClientHandler(Socket socket){
			clientSocket = socket;
		}
		
		/*Method reads two objects from the income connection*/
		@Override
		public void run() {
			Object o1;
			Object o2;
			
			try {
				oit = new ObjectInputStream(clientSocket.getInputStream());
				while((o1 = oit.readObject()) != null){
					o2 = oit.readObject();
					System.out.println("Read two objects");
					tellEveryone(o1, o2);
				}
			} catch (IOException e) {
				System.out.println("Connection reset! IOException");
			} catch (ClassNotFoundException e) {
				System.out.println("Connection reset! ClassNotFoundException");
			}
		}	
	}
	
	/*Establish server at defined port. Accepts client connections and receive/send info.*/
	public void createConnetcion(){
		clientsMessages = new ArrayList<>();
		
		try {
			ServerSocket server = new ServerSocket(5000);
			while(true){
			Socket client = server.accept();
			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
			clientsMessages.add(out);
			
			Thread thread01 = new Thread(new ClientHandler(client));
			thread01.start();
			
			System.out.println("Got a connection!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*Method sends received info to all the server clients*/
	private void tellEveryone(Object o1, Object o2){
		Iterator iterator = clientsMessages.iterator();
		while(iterator.hasNext()){
			try {
				ObjectOutputStream out = (ObjectOutputStream) iterator.next();
				out.writeObject(o1);
				out.writeObject(o2);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new CyberBeatBoxServer().createConnetcion();
	}

}
