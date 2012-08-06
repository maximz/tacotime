package com.yulaev.tacotime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import com.yulaev.tacotime.gamelogic.GameInfo;
import com.yulaev.tacotime.gamelogic.GameLevel;
import com.yulaev.tacotime.gamelogic.Interaction;
import com.yulaev.tacotime.gamelogic.leveldefs.GameLevel_1;
import com.yulaev.tacotime.gamelogic.leveldefs.GameLevel_2;
import com.yulaev.tacotime.gameobjects.Blender;
import com.yulaev.tacotime.gameobjects.CoffeeGirl;
import com.yulaev.tacotime.gameobjects.CoffeeMachine;
import com.yulaev.tacotime.gameobjects.CustomerQueue;
import com.yulaev.tacotime.gameobjects.GameFoodItem;
import com.yulaev.tacotime.gameobjects.ViewObject;
import com.yulaev.tacotime.gameobjects.GameItem;


/**
 * @author iyulaev
 * 
 * This thread handles all of the game logic. That is, whenever some two objects, usually the CoffeeGirl
 * and another GameItem, interact, this thread determines the result of the interaction. It also controls
 * the state of CoffeeGirl and increments points based on the result of in-game interactions.
 */
public class GameLogicThread extends Thread {
	
	private static final String activitynametag = "GameLogicThread";
	
	//Define types of messages accepted by ViewThread
	public static final int MESSAGE_INTERACTION_EVENT = 0;
	public static final int MESSAGE_TICK_PASSED = 1;

	//This message handler will receive messages, probably from the UI Thread, and
	//update the data objects and do other things that are related to handling
	//game-specific input
	public static Handler handler;
	
	public CoffeeGirl coffeeGirl;
	public CustomerQueue customerQueue;
	public HashMap<String, GameItem> gameItems;
	public HashMap<String, GameFoodItem> foodItems;
	
	ViewThread viewThread;
	TimerThread timerThread;
	InputThread inputThread;
	GameLogicThread gameLogicThread;
	Context caller;
	
	//Used to time various things, like pre-level and post-level "announcements"
	int message_timer;
	
	/** Mostly just initializes the Handler that receives and acts on in-game interactions that occur */
	public GameLogicThread(ViewThread viewThread, TimerThread timerThread, InputThread inputThread, Context caller) {
		super();
		
		//Set pointers to all of the other threads
		//This is useful when we want to load a new level
		this.viewThread = viewThread;
		this.timerThread = timerThread;
		this.inputThread = inputThread;
		this.caller = caller;
		
		//"initialize" GameInfo
		GameInfo.money = 0;
		GameInfo.points = 0;
		GameInfo.setLevel(0);
		GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_PREPLAY); //TODO: this should probably be initialized elsewhere
		
		gameItems = new HashMap<String, GameItem>();
		foodItems = new HashMap<String, GameFoodItem>();
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//Handle messages from viewThread that tell the GLT that an interaction between a GameActor and
				//a GameItem has occured
				if(msg.what == MESSAGE_INTERACTION_EVENT) {
					String interactee = (String)msg.obj;
					Log.d(activitynametag, "Got interaction event message! Actor interacted with " + interactee);
					
					//Attempt interaction and see if interactee changed state
					Interaction interactionResult = gameItems.get(interactee).onInteraction(coffeeGirl.getItemHolding());
					
					//If the interaction resulted in a state change, OR was successful (when interacting with CustomerQueue),
					//change coffeegirl state
					if(interactionResult.previous_state != -1 || interactionResult.was_success) {
						//coffeeGirl.setState(coffeeGirlNextState(coffee_girl_prev_state, interactee, interactee_state));
						coffeeGirlNextState(coffeeGirl.getState(), interactee, interactionResult);
					}
				}
				
				//Handle messages from timerThread that tell the GameLogicThread that a second has passed
				//This is the main state machine for the GameLogicThread! Which controls how the game works!
				//So this is the most important part of the game!!!
				if(msg.what == MESSAGE_TICK_PASSED) {
					stateMachineClockTick();
				}
			}
		};		
	}
	
	public void setSelf(GameLogicThread gameLogicThread) { this.gameLogicThread = gameLogicThread; }
	
	/** Since CoffeeGirl interacts with all other game items, describing the CoffeeGirl state machine is done on the global level
	 * rather than within CoffeeGirl itself. As a side effect GameInfo money and/or points may change depending on how
	 * CoffeeGirl's state has changed
	 * @param old_state The previous state of coffee girl
	 * @param interactedWith The ID of the GameItem CoffeeGirl interacted with
	 * @param interactee_state The state of the GameItem that CoffeeGirl interacted with
	 * @return What the next state should be, (if any change occurs)
	 */
	public void coffeeGirlNextState(int old_state, String interactedWith, Interaction interactionResult) {
		int interactee_state = interactionResult.previous_state;
		
		//CoffeeGirl's hands are empty, she interacts with a coffeemachine that is done -> she is now carrying coffee
		if(old_state == CoffeeGirl.STATE_NORMAL && 
				interactedWith.contains("CoffeeMachine") && 
				interactee_state == CoffeeMachine.STATE_DONE) coffeeGirl.setItemHolding("coffee");
		
		//CoffeeGirl's hands are empty, she interacts with a cupcake tray -> she is now carrying a cupcake
		if(old_state == CoffeeGirl.STATE_NORMAL && 
				interactedWith.equals("CupCakeTray")) coffeeGirl.setItemHolding("cupcake");
		
		//CoffeeGirl has a coffee, she interacts with blender -> she now has nothing
		if(old_state == CoffeeGirl.STATE_CARRYING_COFFEE && 
				interactedWith.equals("Blender")) coffeeGirl.setItemHolding("nothing");
		
		//CoffeeGirl's hands are empty, she interacts with a blender that is done -> she is now carrying blended drink
		if(old_state == CoffeeGirl.STATE_NORMAL && 
				interactedWith.equals("Blender") && 
				interactee_state == Blender.STATE_DONE) coffeeGirl.setItemHolding("blended_drink");
		
		//CoffeeGirl's hands are NOT empty, she interacts with trashcan -> hands now empty, increment money and/or points
		if(old_state != CoffeeGirl.STATE_NORMAL && 
				interactedWith.equals("TrashCan")) {
			GameInfo.setAndReturnPoints(foodItems.get(coffeeGirl.getItemHolding()).pointsOnInteraction(interactedWith, 0));
			GameInfo.setAndReturnMoney(foodItems.get(coffeeGirl.getItemHolding()).moneyOnInteraction(interactedWith, 0)); 
			coffeeGirl.setItemHolding("nothing");
		}
		
		//CoffeeGirl interacts with CustomerQueue - if the interaction is successful then CoffeeGirl loses the
		//item that she currently holds and gains some points in return
		if(old_state != CoffeeGirl.STATE_NORMAL && 
				interactedWith.equals("CustomerQueue") &&
				interactionResult.was_success) {
			GameInfo.setAndReturnPoints(interactionResult.point_result);
			GameInfo.setAndReturnMoney(interactionResult.money_result);
			coffeeGirl.setItemHolding("nothing");
		}
		
		//Default case - don't change state!
		//return(old_state);
	}

	/** Updates this GameLogicThread's state machine. Should be called every time a clock tick (nominally one
	 * real-time second) occurs
	 * 
	 * @sideeffect Mucks with GameInfo and MessageRouter to update game state and inform other Threads
	 * about the updates in the game state.
	 */
	public void stateMachineClockTick() {
		//IF we are viewing the main panel AND we are ready to play a level, this means the
		//game is ready for another level - load one!
		if(GameInfo.getGameMode() == GameInfo.MODE_MAINGAMEPANEL_PREPLAY) {
			loadLevel(GameInfo.getLevel() + 1);
			
			message_timer = 3;
			MessageRouter.sendAnnouncementMessage("Level Start in " + message_timer, true);
			
			Log.v(activitynametag, "GLT is loading a new level!");
			GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_PREPLAY_MESSAGE);
		}
		
		else if(GameInfo.getGameMode() == GameInfo.MODE_MAINGAMEPANEL_PREPLAY_MESSAGE) {
			if(message_timer > 0) {
				message_timer--;
				MessageRouter.sendAnnouncementMessage("Level Start in " + message_timer, true);
			}
			else {
				GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_INPLAY);
				MessageRouter.sendAnnouncementMessage("", false); //remove the announcement message
				MessageRouter.sendPauseMessage(false); //unpauses ViewThread and InputThread
				Log.v(activitynametag, "GLT is starting a new level!");
			}
		}
		
		
		//If we are in play check to see if we should finish the level
		else if(GameInfo.getGameMode() == GameInfo.MODE_MAINGAMEPANEL_INPLAY) {
			GameInfo.decrementLevelTime();
			
			Log.v(activitynametag, GameInfo.getLevelTime() + " seconds remaining in this level!");
			
			//If we've run out of time on this level, or customerQueue has run out, then kill the level
			if(GameInfo.getLevelTime() <= 0 || customerQueue.isFinished()) {
				Log.v(activitynametag, "GLT is finishing this level!");
				
				message_timer = 3;
				MessageRouter.sendAnnouncementMessage("Level " + GameInfo.getLevel() + " Finished", true); //remove the announcement message
				GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_POSTPLAY_MESSAGE);
			}
		}
		
		else if(GameInfo.getGameMode() == GameInfo.MODE_MAINGAMEPANEL_POSTPLAY_MESSAGE) {
			if(message_timer > 0) {
				message_timer--;
			}
			else {
				MessageRouter.sendPauseMessage(true);
				//GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_POSTPLAY);
				GameInfo.setGameMode(GameInfo.MODE_MAINGAMEPANEL_PREPLAY);
				//TODO - message to the MenuThread to bring up a popup?
			}
		}
	}
	
	/** Sets the actor associated with this GameLogicThread
	 * 
	 * @param n_actor The CoffeeGirl Object that will be this game's Actor, i.e. the player-controlled character
	 */
	public void setActor(CoffeeGirl n_actor) {
		coffeeGirl = n_actor;
	}
	
	public void setCustomerQueue(CustomerQueue n_customerQueue) {
		customerQueue = n_customerQueue;
		this.addGameItem(customerQueue);
	}
	
	 /** Adds a GameItem to this TacoTime game. The GameItem will be put into the gameItems data structure 
	  * so that it's state can be updated when an interaction occurs. 
	  * @param n_gameItem The GameItem to put into the gameItems map.
	  */
	public void addGameItem(GameItem n_gameItem) {
		gameItems.put(n_gameItem.getName(), n_gameItem);
	}
	
	/** This method is typically called by MainGamePanel when adding new GameFoodItems into the game. Basically 
	 * we add a new type of food item and an associated CoffeeGirl state.
	 * @param foodItem The GameFoodItem to add to this game.
	 * @param associated_coffeegirl_state The associated state that CoffeeGirl will be put into when she 
	 * receives the FoodItem.
	 */
	public void addNewFoodItem(GameFoodItem foodItem, int associated_coffeegirl_state) {
		boolean doSetItemHolding = false;
		if(foodItems.isEmpty()) doSetItemHolding = true;
		
		foodItems.put(foodItem.getName(), foodItem);
		coffeeGirl.setItemHoldingToStateAssoc(foodItem.getName(), associated_coffeegirl_state);
		
		//If CoffeeGirl's "held item" hasn't been set up yet then set it to the first foodItem that we add
		//better hope that the first one we add is "nothing"!
		if(doSetItemHolding) coffeeGirl.setItemHolding(foodItem.getName());
	}
	
	/** Return a List of the GameFoodItems that have been added to this GameLogicThread. Utility method that
	 * is a bit out of place but it is convenient since we have to explain the GameFoodItem -> CoffeeGirl State
	 * mapping to GTL anyway and so it implicitly gets a List of the GameFoodItems that are valid in this game.
	 * 
	 * @return A List of GameFoodItems valid for this game/level/whatever.
	 */
	public List<GameFoodItem> getFoodItems() {
		ArrayList<GameFoodItem> retval = new ArrayList<GameFoodItem>();
		Iterator<String> it = foodItems.keySet().iterator();
		while(it.hasNext()) retval.add(foodItems.get(it.next()));
		
		return(retval);
	}
	
	/**Clears all of the gameItems, foodItems, etc in preparation to load a new level.
	 * 
	 */
	public void reset() {
		coffeeGirl = null;
		gameItems = new HashMap<String, GameItem>();
		foodItems = new HashMap<String, GameFoodItem>();
	}
	
	@Override
	public void run() {
		
		;

	}
	
	/** Loads a new level; creates a GameLevel Object corresponding to the new level
	 * and kicks off (unpauses) all of the threads.
	 * TODO: Should this really be done in the GameLogicThread or should we have a dedicated loader thread?
	 * @param levelNumber
	 */
	public void loadLevel(int levelNumber) {
		GameLevel newLevel = null;
		if(levelNumber == 1) {
			//Launch level 1!
			newLevel = new GameLevel_1();
			newLevel.loadLevel(viewThread, gameLogicThread, inputThread, this.caller);
		}
		else if(levelNumber == 2) {
			//Launch level 2!
			newLevel = new GameLevel_2();
			newLevel.loadLevel(viewThread, gameLogicThread, inputThread, this.caller);
		}
		
		//Set up the game state
		GameInfo.setLevel(levelNumber);
		
		if(newLevel == null) 
			Log.d(activitynametag, "newLevel was unexpectedly null! levelNumber = " + levelNumber);
		else
			GameInfo.setLevelTime(newLevel.getLevelTime());
	}
	
}
