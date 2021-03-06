/** This is the main View (SurfaceView) of the TacoTime game engine. In addition to implementing 
 * the UI it also creates all of the game Threads, and launches everything. GameLogicThread (now)
 * deals with loading all of the levle data, setting up the GameItems, etc.
 * 
 * @author iyulaev
 */

package org.coffeecats.coffeetime;

import java.util.ArrayList;
import java.util.Iterator;

import org.coffeecats.coffeetime.gamelogic.GameGrid;
import org.coffeecats.coffeetime.gameobjects.ViewObject;

import org.coffeecats.coffeetime.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class MainGamePanel extends SurfaceView implements SurfaceHolder.Callback {

	private static final String activitynametag = MainGamePanel.class.getSimpleName();
	
	//The below are the UI-related threads that are involved in handling input and rendering the game
	ViewThread viewThread;
	InputThread inputThread;
	GameLogicThread gameLogicThread;
	TutorialThread tutorialThread;
	SoundThread soundThread;
	
	//Make sure we don't double-make the threads
	boolean threads_launched;
	
	//Parent Activity context
	Context context;
	
	//Will be used for positioning toasts
	//int toast_position_y;

	/** Constructor for MainGamePanel. Mostly this sets up and launches all of the game threads.
	 * 
	 * @param context The context that creates this MainGamePanel
	 * @param load_saved_game Whether to load a saved game or not
	 */
	public MainGamePanel(Context context, boolean load_saved_game, boolean watch_tutorial) {
		super(context);
		this.context = context;
		
		// adding the callback (this) to the surface holder to intercept events
		getHolder().addCallback(this);
		
		// create the view thread that will be responsible for updating the canvas
		viewThread = new ViewThread(getHolder(), this);
		MessageRouter.viewThread = viewThread;
		
		// create the input handler thread
		inputThread = new InputThread();
		MessageRouter.inputThread = inputThread;
		
		//create sound thread
		soundThread = new SoundThread(context);
		MessageRouter.soundThread = soundThread;
		
		//create the Game Logic Thread
		gameLogicThread = new GameLogicThread(viewThread, inputThread, this.getContext(), load_saved_game,
					watch_tutorial ? -1 : 0);
		gameLogicThread.setSelf(gameLogicThread);
		MessageRouter.gameLogicThread = gameLogicThread;
		
		//Create the tutorial thread IF NECESSARY
		if(watch_tutorial)
			tutorialThread = new TutorialThread();
		
		threads_launched = false;
		
		//Calculate the position that we should use to place announcements
		//Shoudl be placed below the game grid
		//CURRENTLY WE AREN'T USING THE TOASTS TO DISPLAY ANNOUNCEMENTS
		/* WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int display_height = display.getHeight();
		toast_position_y = display_height - GameGrid.canvasX(GameGrid.GAMEGRID_HEIGHT); */
		
		//Log.v(activitynametag, "GameGrid (player area) = " + GameGrid.canvasY(GameGrid.GAMEGRID_WIDTH) + "," + GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT - 33));
		
		// make the GamePanel focusable so it can handle events
		setFocusable(true);
	}
	
	/** called when we are to destroy this MainGamePanel; mostly in charge of freeing resources by calling the
	 * "destructor" methods of the various threads we've created
	 */
	public void destroy() {
		soundThread.destroy();
	}

	/** Not sure why this is here right now but I suppose it is to handle changes like a change
	 * in orientation (which we should suppress anyway!). This is implemented here only because 
	 * we implement SurfaceHolder.Callback.
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	/** In this method we perform pretty much all of the initialization and placement of in-game items.
	 * We create Objects for all of the in-game items, set up the game character (CoffeeGirl), add all
	 *  of the item's to the various thread's sensitivity lists, and kick off all of the threads once 
	 *  this is done.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(activitynametag, "MainGamePanel surfaceCreated() called!");
		
		//If we've not launched all of the Threads then do so
		if(!threads_launched) {
			// at this point the surface is created and
			// we can safely start the game loop
			
			//Kick off all of the threads
			viewThread.start();
			inputThread.start();
			gameLogicThread.start();
			//timerThread.start();
			soundThread.start();
			if(tutorialThread != null)
				tutorialThread.start();
			
			threads_launched = true;
		} 
		//Otherwise just un-suspend the timer and view threads (since they exist already...right?)
		else {
			MessageRouter.sendSuspendTimerThreadMessage(false);
			MessageRouter.sendSuspendViewThreadMessage(false);
			MessageRouter.sendSuspendSoundThreadMessage(false);
		}
	}

	/** This method winds down all of the threads. */
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(activitynametag, "MainGamePanel surfaceDestroyed() called!");
		
		//Pause the threads that have event loops built in
		MessageRouter.sendSuspendTimerThreadMessage(true);
		MessageRouter.sendSuspendViewThreadMessage(true);
		MessageRouter.sendSuspendSoundThreadMessage(true);
	}
	
	/** Responds to a touch event; mostly just sends the tap event to the InputThread via MessageRouter. 
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Log.d(activitynametag, "Coords: x=" + event.getX() + ",y=" + event.getY());
			MessageRouter.sendInputTapMessage((int)event.getX(), (int)event.getY());
		}
		return super.onTouchEvent(event);
	}
	
	boolean static_setup_done = false;
	//Paint that we use in onDraw(); these are class variables so that we don't keep creating new ones
	Paint gridPaint;
	//Paint counterPaint;
	Rect counterAreaFullRect;
	
	Paint moneyPaint;
	Paint pointsPaint;
	Paint levelTimePaint;
	Paint customersLeftPaint;
	
	Paint announcementPaint;
	Paint announcementItalicPaint;
	//Bitmap that we will use for the background
	Bitmap background;
	//Bitmaps we'll use for counter top
	Bitmap counterTopFull;
	
	//Used for displaying toasts
	/* private Toast t;
	private int toast_trim = 40;
	private long time_since_last_toast = -1;
	private final int TIME_BETWEEN_TOASTS = 1000; */
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
	}

	/** This onDraw() method will draw all of the ViewObjects in the game; it is the main onDraw() method
	 * that gets called by the ViewThread. It can also draw announcement messages that overlay the
	 * rest of the game.
	 * 
	 * @param canvas The Canvas to draw everything onto
	 * @param voAr An ArrayList of ViewObjects that will be drawn
	 * @param money The amount of money the player has so that we can draw it
	 * @param points The number of points the player has so that we can draw it
	 * @param boolean draw_announcement_message Whether or not to draw an announcement; it is a message that
	 * overlays everything else on the screen
	 * @param announcementMessage The announcement message to draw; will not be drawn (and may be null) if
	 * draw_announcement_message is set to false
	 */
	
	protected void onDraw(Canvas canvas, ArrayList<ViewObject> voAr, 
			int money, int points, 
			boolean draw_announcement_message, String announcementMessage,
			int level_time,
			int customers_left,
			int customers_until_bonus,
			int customer_until_cleared,
			boolean use_italic_announcement_font) {
		
		//Set up all of the Paint objects if we haven't done this yet! (should only happen once per game)
		if(!static_setup_done) {
			int status_text_size = 20;
			int announcement_italic_text_size = 24;
			int announcement_text_size = 28;
			
			gridPaint = new Paint();
			gridPaint.setColor(0xFF2f2f2f);
			
			/*playAreaRect = new Rect(0,
					0,
					GameGrid.canvasX(GameGrid.GAMEGRID_WIDTH),
					GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT));*/
			background = BitmapFactory.decodeResource(this.getResources(), R.drawable.background_brown);
			int background_side = (GameGrid.canvasX(GameGrid.GAMEGRID_WIDTH)>GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT)) ? 
					GameGrid.canvasX(GameGrid.GAMEGRID_WIDTH) : 
					GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT);
			background = Bitmap.createScaledBitmap(background, 
					background_side,background_side,
					false);
			
			counterAreaFullRect = new Rect(0,0,
					canvas.getWidth(),
					GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT-25));
			
			counterTopFull = BitmapFactory.decodeResource(this.getResources(), R.drawable.countertop_full);
			counterTopFull = Bitmap.createScaledBitmap(counterTopFull, 
					canvas.getWidth(),GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT-25),
					false);
			
			moneyPaint = new Paint();
			moneyPaint.setColor(Color.GREEN);
			moneyPaint.setTextSize(status_text_size);
			pointsPaint = new Paint();
			pointsPaint.setColor(Color.BLUE);
			pointsPaint.setTextSize(status_text_size);
			levelTimePaint = new Paint();
			levelTimePaint.setColor(Color.RED);
			levelTimePaint.setTextSize(status_text_size);
			announcementPaint = new Paint();
			announcementPaint.setColor(0xFFDFDFDF);
			announcementPaint.setTextSize(announcement_text_size);
			announcementPaint.setTextAlign(Paint.Align.CENTER);
			announcementItalicPaint = new Paint();
			announcementItalicPaint.setColor(0xFFCFCFCF);
			announcementItalicPaint.setTextSize(announcement_italic_text_size);
			announcementItalicPaint.setTextAlign(Paint.Align.CENTER);
			announcementItalicPaint.setTextSkewX(-0.20f);
			customersLeftPaint = new Paint();
			customersLeftPaint.setColor(0xFFDFDFDF);
			customersLeftPaint.setTextSize(status_text_size);
			
			static_setup_done = true;
		}
		
		canvas.drawColor(Color.BLACK);
		
		//Draw in a background
		//canvas.drawBitmap(background, null, playAreaRect, null);
		canvas.drawBitmap(background, 0,0, null);
		canvas.drawRect(0, GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT), GameGrid.maxCanvasX(), canvas.getHeight(), gridPaint);
		//Draw countertop 
		canvas.drawBitmap(counterTopFull, 0,GameGrid.canvasY(25), null);
		
		
		
		
		//Draw ALL ViewObjects on the board
		Iterator<ViewObject> it = voAr.iterator();
		while(it.hasNext()) {
			it.next().draw(canvas);
		}
		
		
		final int SPACE_BETWEEN_ANNOUNCEMENT_LINES = 40;
		
		//Draw information regarding how many customers are left
		String customersLeftStr;
		if(customer_until_cleared > 0) {
			customersLeftStr = new String("Customers Left: " + Integer.toString(customer_until_cleared));
		} else if (customers_until_bonus > 0) {
			customersLeftStr = new String("Customers Left: " + Integer.toString(customers_until_bonus) + " (level cleared)");
		} else {
			customersLeftStr = new String("Customers Left: " + Integer.toString(customers_left) + " (bonus achieved)");
		}
		
		canvas.drawText(customersLeftStr, 14, canvas.getHeight()-15-SPACE_BETWEEN_ANNOUNCEMENT_LINES*3, customersLeftPaint);
		
		//Draw money, points and display an announcement message IF there is an announcement
		canvas.drawText("Time Left: " + Integer.toString(level_time), 14, canvas.getHeight()-15-SPACE_BETWEEN_ANNOUNCEMENT_LINES*2, levelTimePaint);
		canvas.drawText("Money: $" + Integer.toString(money), 14, canvas.getHeight()-15-SPACE_BETWEEN_ANNOUNCEMENT_LINES*1, moneyPaint);
		canvas.drawText("Points: " + Integer.toString(points), 14, canvas.getHeight()-15, pointsPaint);
		
		if(draw_announcement_message) {
			canvas.drawText(announcementMessage, 
					canvas.getWidth()/2, GameGrid.canvasY(GameGrid.GAMEGRID_HEIGHT) + 40,
					use_italic_announcement_font?announcementItalicPaint:announcementPaint);
			
			/*if(time_since_last_toast + TIME_BETWEEN_TOASTS < SystemClock.uptimeMillis()) {
				time_since_last_toast = SystemClock.uptimeMillis();
				
				if(t != null) {
					t.cancel();
					t = null;
				}
				
				t = Toast.makeText(context, announcementMessage, Toast.LENGTH_SHORT);
				t.setGravity(Gravity.BOTTOM, 0, -1 * toast_position_y + toast_trim);
				t.show();
			}*/
		}
		
	}

}
