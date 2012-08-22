package com.yulaev.tacotime;


import java.util.ArrayList;
import java.util.Iterator;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yulaev.tacotime.gameobjects.ViewObject;


/**
 * @author iyulaev
 * 
 * This thread handles user input. It receives messages, mostly from MessageRouter, and handles 
 * them appropriately. For example, on user taps, we call handleTap() on all ViewObjects that have
 * been put into this InputThread's viewObjects array.
 */
public class InputThread extends Thread {
	
	private static final String activitynametag = "InputThread";
	
	//Define types of messages accepted by ViewThread
	public static final int MESSAGE_HANDLE_ONTAP = 0;
	public static final int MESSAGE_SET_PAUSED = 1;
	public static final int MESSAGE_SET_UNPAUSED = 2;
	public static final int MESSAGE_INGAME_DIALOG_LAUNCHED = 3;
	public static final int MESSAGE_INGAME_DIALOG_FINISHED = 4;
	
	//Define the possible results for the in-game dialog
	public static final int INGAMEDIALOGRESULT_MAIN_MENU = 0;
	public static final int INGAMEDIALOGRESULT_RETRY_LEVEL = 1;
	public static final int INGAMEDIALOGRESULT_CONTINUE = 2;

	//This message handler will receive messages, probably from the UI Thread, and
	//update the data objects and do other things that are related to handling
	//game-specific input
	public static Handler handler;
	
	//Here we hold all of the objects that the ViewThread must update
	ArrayList<ViewObject> viewObjects;
	
	//True if this thread is running and sending input results through message router
	private boolean paused;

	/** Mostly just sets up a Handler that receives messages from the main game Activity 
	 * and calls the handleTap() methods for all of the ViewObjects in the game screen */
	public InputThread() {
		super();
		this.viewObjects = new ArrayList<ViewObject>();
		paused = false;
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MESSAGE_HANDLE_ONTAP) {
					Log.d(activitynametag, "Got input message!");
					//If we are paused, do not propagate user input to rest of the game
					if(!paused) handleTap(msg.arg1, msg.arg2);
				}
				
				else if (msg.what == MESSAGE_SET_PAUSED) setPaused(true);
				else if (msg.what == MESSAGE_SET_UNPAUSED) setPaused(false);
				else if (msg.what == MESSAGE_INGAME_DIALOG_LAUNCHED) {
					MessageRouter.sendPauseMessage(true);
					setPaused(true);
				}
				else if (msg.what == MESSAGE_INGAME_DIALOG_FINISHED) {
					setPaused(false);
					MessageRouter.sendPauseMessage(false);
					int result = msg.arg1;
					
					//debugging only...
					/*String dialogResultString = "null";
					if(result == InputThread.INGAMEDIALOGRESULT_MAIN_MENU) dialogResultString = "main menu";
					if(result == InputThread.INGAMEDIALOGRESULT_RETRY_LEVEL) dialogResultString = "retry level";
					if(result == InputThread.INGAMEDIALOGRESULT_CONTINUE) dialogResultString = "continue";
					Log.d(activitynametag, "Dialog result was: " + dialogResultString);*/
					
					//on continue, do nothing
					
					//On "retry level" have GTL load the last saved game
					if(result == INGAMEDIALOGRESULT_RETRY_LEVEL) {
						MessageRouter.sendLoadGameMessage();
					}
					
					//on "return to main menu" does nothing, since the TTMGA will handle destroying TTMGA
					else if(result == INGAMEDIALOGRESULT_MAIN_MENU); 
				}
			}
		};		
	}

	/** Add a new ViewObject to this InputThread's ViewObject data structure. All VOs added
	 * to this InputThread will have user input passed to them via handleTap() and other
	 * such methods.
	 * @param nVO The new ViewObject to add to this InputThread's ViewObject sensitivity list.
	 */
	public void addViewObject(ViewObject nVO) {
		viewObjects.add(nVO);
	}
	
	/** Clear all VOs by creating a new viewObjects array list
	 * Done between levels to  "reset" the game
	 */
	public synchronized void reset() {
		viewObjects = new ArrayList<ViewObject>();
	}
	
	/** This method is called when a HANDLE_ONTAP message is received by this InputThread. It calls 
	 * handleTap() for all ViewObjects present in the game so that they are aware that user input 
	 * has occured.
	 * 
	 * @param x The x co-ordinate of the user tap
	 * @param y The y co-ordinate of the user tap
	 */
	public void handleTap(int x, int y) {
		Iterator<ViewObject> it = viewObjects.iterator();
		while(it.hasNext()) it.next().handleTap(x,y);
	}
	
	/** Does nothing! Everything is handled in the handler.
	 */
	@Override
	public void run() {
		
		;

	}
	
	/** Toggles the "paused" member variable. When paused is true then no handleTap calls are made and 
	 * therefore no user interface inputs get propagated to the ViewObjects (various items) of the game.
	 * @param n_paused The new setting for paused.
	 */
	public void setPaused(boolean n_paused) {
		this.paused = n_paused;
	}
	
}
