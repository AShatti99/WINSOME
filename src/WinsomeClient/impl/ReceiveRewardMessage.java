package WinsomeClient.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ReceiveRewardMessage implements Runnable{
	
	MulticastSocket multicastSocket;
  
	public ReceiveRewardMessage(MulticastSocket multicastSocket){
		
		this.multicastSocket = multicastSocket;
	}
	
	@Override
	public void run() {
		
		try {
			while (!multicastSocket.isClosed()){
				DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
				multicastSocket.receive(packet);
				String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
				System.out.println("\n< " + msg);
				System.out.print("> ");
			}
		} catch (IOException ignored) {}
	}
}
