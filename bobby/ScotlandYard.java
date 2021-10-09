package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScotlandYard implements Runnable{

	/*
		this is a wrapper class for the game.
		It just loops, and runs game after game
	*/

	public int port;
	public int gamenumber;

	public ScotlandYard(int port){
		this.port = port;
		this.gamenumber = 0;
	}

	public void run(){
		while (true){
			this.gamenumber++;
			Thread tau = new Thread(new ScotlandYardGame(this.port, this.gamenumber));
			tau.start();
			try{
				tau.join();
			}
			catch (InterruptedException e){
				return;
			}
			// System.out.println(gamenumber);
			
		}
	}

	public class ScotlandYardGame implements Runnable{
		private Board board;
		private ServerSocket server;
		public int port;
		public int gamenumber;
		private ExecutorService threadPool;

		public ScotlandYardGame(int port, int gamenumber){
			this.port = port;
			this.board = new Board();
			this.gamenumber = gamenumber;
			try{
				this.server = new ServerSocket(port);
				System.out.println(String.format("Game %d:%d on", port, gamenumber));
				server.setSoTimeout(5000);
			}
			catch (IOException i) {
				return;
			}
			this.threadPool = Executors.newFixedThreadPool(10);
		}


		public void run(){
			try{
				//INITIALISATION: get the game going
				Socket socket = null;
				boolean fugitiveIn=false;
				board.dead=false;
				// listen for a client to play fugitive, and spawn the moderator.
				// here, it is actually ok to edit this.board.dead, because the game hasn't begun
				do{
					try {
						socket=server.accept();
						board.totalThreads++;
						threadPool.execute(new ServerThread(board, -1, socket, port, gamenumber));
						fugitiveIn=true;
						
					} 
					catch (SocketTimeoutException t){
						if(!board.dead){
							continue;
						}                          	
					}	
				} while (!fugitiveIn);
				
				// Spawn a thread to run the Fugitive
				// Spawn the moderator
				threadPool.execute(new Moderator(board));
				while (true){
					try {
						socket=server.accept();
					} 
					catch (SocketTimeoutException t){
						if(!board.dead){
							continue;
						}                          	
					}
					int id=board.getAvailableID();
					
					if(board.dead){
						break;
					}else if(id==-1){
						continue;
					}
					else{
						// System.out.println("Hello\n");
						board.threadInfoProtector.acquire();
						board.totalThreads++;
						threadPool.execute(new ServerThread(board, id, socket, port, gamenumber));	
						this.board.threadInfoProtector.release();
						this.board.moderatorEnabler.release();
						continue;
					}
					
					// acquire thread info lock, and decide whether you can serve the connection at this moment,
					// if you can't, drop connection (game full, game dead), continue, or break.
					// if you can, spawn a thread, assign an ID, increment the totalThreads
					// don't forget to release lock when done!                             
				}		
				// reap the moderator thread, close the server, 
				// kill threadPool (Careless Whispers BGM stops)
				System.out.println(String.format("Game %d:%d Over", this.port, this.gamenumber));
				return;
			}
			catch (InterruptedException ex){
				System.err.println("An InterruptedException was caught: " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			catch (IOException i){
				return;
			}		
		}	
	}

	public static void main(String[] args) {
		for (int i=0; i<args.length; i++){
			int port = Integer.parseInt(args[i]);
			Thread tau = new Thread(new ScotlandYard(port));
			tau.start();
		}
	}
}
