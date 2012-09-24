package com.yulaev.tacotime.gamelogic.leveldefs;

import android.content.Context;

import com.yulaev.tacotime.GameLogicThread;
import com.yulaev.tacotime.InputThread;
import com.yulaev.tacotime.R;
import com.yulaev.tacotime.ViewThread;
import com.yulaev.tacotime.gamelogic.CustomerQueueWrapper;
import com.yulaev.tacotime.gamelogic.GameGrid;
import com.yulaev.tacotime.gamelogic.GameInfo;
import com.yulaev.tacotime.gamelogic.GameLevel;
import com.yulaev.tacotime.gameobjects.CoffeeGirl;
import com.yulaev.tacotime.gameobjects.CustomerQueue;
import com.yulaev.tacotime.gameobjects.GameItem;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemBlendedDrink;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemCoffee;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemCupcake;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemEspresso;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemNothing;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemPieSlice;
import com.yulaev.tacotime.gameobjects.fooditemdefs.FoodItemSandwich;
import com.yulaev.tacotime.gameobjects.objectdefs.Blender;
import com.yulaev.tacotime.gameobjects.objectdefs.CoffeeMachine;
import com.yulaev.tacotime.gameobjects.objectdefs.CounterTop;
import com.yulaev.tacotime.gameobjects.objectdefs.CupCakeTray;
import com.yulaev.tacotime.gameobjects.objectdefs.EspressoMachine;
import com.yulaev.tacotime.gameobjects.objectdefs.Microwave;
import com.yulaev.tacotime.gameobjects.objectdefs.PieTray;
import com.yulaev.tacotime.gameobjects.objectdefs.SoundSystem;
import com.yulaev.tacotime.gameobjects.objectdefs.TrashCan;

/** Describes level #1 for the Coffee Time game! */

public class GameLevel_0 extends GameLevel {
	
	public GameLevel_0() {
		this.level_number = 0;
		this.customerQueue_length = 2;
		this.point_mult = 0.0f;
		this.money_mult = 0.0f;
		this.customer_impatience = 0.25f;
		this.time_limit_sec = 70;
		this.customer_max_order_size = 1;
		
		this.point_bonus = 0;
		this.money_bonus = 0;
		this.point_bonus_derating = 0.5f;
		this.money_bonus_derating = 0.5f;		
	}
	
	/** Set up this level; add all GameItems and such to the Threads, set up the Customers and such
	 * with the per-level parameters.
	 * @param vT ViewThread associated with this game session
	 * @param gLT GameLogicThread associated with this game session
	 * @param iT InputThread associated with this game session
	 * @param caller The calling Context for loading resources and such
	 */
	public void loadLevel(ViewThread viewThread, GameLogicThread gameLogicThread, InputThread inputThread, Context caller) {
		super.loadLevel(viewThread, gameLogicThread, inputThread, caller);
		
		//Setup coffeegirl (actor)
		CoffeeGirl coffeegirl = new CoffeeGirl(caller);
		viewThread.addViewObject(coffeegirl);
		viewThread.setActor(coffeegirl);
		inputThread.addViewObject(coffeegirl);
		gameLogicThread.setActor(coffeegirl);
		
		//Create and add objects to viewThread containers (UPDATE FOR NEW GAMEITEM)
		CoffeeMachine coffeeMachine = new CoffeeMachine(caller, R.drawable.coffeemachine, 
				CoffeeMachine.DEFAULT_XPOS, CoffeeMachine.DEFAULT_YPOS, GameItem.ORIENTATION_WEST);
		//GameItem coffeeMachine = new GameItem(caller, "CoffeeMachine", R.drawable.coffeemachine, 100, 50, GameItem.ORIENTATION_NORTH);
		//viewThread.addViewObject(coffeeMachine);
		viewThread.addGameItem(coffeeMachine);
		inputThread.addViewObject(coffeeMachine);
		gameLogicThread.addGameItem(coffeeMachine);	
		
		if(GameInfo.hasUpgrade("secondcoffeemachine")) {
			coffeeMachine = new CoffeeMachine(caller, R.drawable.coffeemachine, 
					CoffeeMachine.DEFAULT_XPOS, CoffeeMachine.DEFAULT_YPOS+20, GameItem.ORIENTATION_WEST);
			viewThread.addGameItem(coffeeMachine);
			inputThread.addViewObject(coffeeMachine);
			gameLogicThread.addGameItem(coffeeMachine);
		}
		
		if(GameInfo.hasUpgrade("countertop")) {
			CounterTop counterTop = new CounterTop(caller, R.drawable.countertop_grey, 
					CounterTop.DEFAULT_XPOS, CounterTop.DEFAULT_YPOS, GameItem.ORIENTATION_NORTH);
			viewThread.addGameItem(counterTop);
			inputThread.addViewObject(counterTop);
			gameLogicThread.addGameItem(counterTop);
		}
		
		TrashCan trashCan = new TrashCan(caller, R.drawable.trashcan, 
				TrashCan.DEFAULT_XPOS, TrashCan.DEFAULT_YPOS, GameItem.ORIENTATION_EAST);
		//viewThread.addViewObject(trashCan);
		viewThread.addGameItem(trashCan);
		inputThread.addViewObject(trashCan);
		gameLogicThread.addGameItem(trashCan);
		
		CupCakeTray cupcakeTray = new CupCakeTray(caller, R.drawable.cupcake_tray, 
				CupCakeTray.DEFAULT_XPOS, CupCakeTray.DEFAULT_YPOS, GameItem.ORIENTATION_EAST);
		//viewThread.addViewObject(cupcakeTray);
		viewThread.addGameItem(cupcakeTray);
		inputThread.addViewObject(cupcakeTray);
		gameLogicThread.addGameItem(cupcakeTray);
		
		//Set up all Food Items (UPDATE FOR NEW FOODITEM)
		gameLogicThread.addNewFoodItem(new FoodItemNothing(caller), CoffeeGirl.STATE_NORMAL);
		gameLogicThread.addNewFoodItem(new FoodItemCoffee(caller), CoffeeGirl.STATE_CARRYING_COFFEE);
		gameLogicThread.addNewFoodItem(new FoodItemCupcake(caller), CoffeeGirl.STATE_CARRYING_CUPCAKE);
		
		//Magic numbers: 40 - x-position of Customers, (GameGrid.GAMEGRID_HEIGHT-45) - y-position of customers
		//1 - starting customer queue length, 
		CustomerQueue custQueue = new CustomerQueue(caller, 40, GameGrid.GAMEGRID_HEIGHT-40, GameItem.ORIENTATION_SOUTH, 
				customerQueue_length, point_mult, money_mult, 
				customer_impatience, customer_max_order_size, 
				gameLogicThread.getFoodItems());
		
		//Tutorial level pre-sets the customer orders
		custQueue.setCustomerOrder(0, new FoodItemCoffee(caller));
		custQueue.setCustomerOrder(1, new FoodItemCupcake(caller));
		
		//viewThread.addViewObject(custQueue);
		viewThread.addGameItem(custQueue);
		inputThread.addViewObject(custQueue);
		gameLogicThread.setCustomerQueue(new CustomerQueueWrapper(custQueue));
	}
}
