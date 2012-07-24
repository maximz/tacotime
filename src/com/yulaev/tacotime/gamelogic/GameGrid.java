package com.yulaev.tacotime.gamelogic;

import android.util.Log;

/** This class is used to translate from real, on-screen co-ordinates to the internal
 * game grid. The virtual game grid is used to detect interactions and place objects, and is
 * translated to the real canvas pixel positions during the rendering process.
 * @author ivany
 *
 */

public class GameGrid {
	public static final int GAMEGRID_PADDING = 30;
	public static final int GAMEGRID_WIDTH = 80+(2*GAMEGRID_PADDING);
	public static final int GAMEGRID_HEIGHT = 100+(2*GAMEGRID_PADDING);
	
	
	private static int canvas_width, canvas_height;
	private static int canvas_anchor_x, canvas_anchor_y;
	//scaling factor when going from real canvas points to game grid points
	private static float scaling_factor; 
	
	/** This "constructor" takes the real canvas pixel dimensions and calculates the appropriate scaling factor
	 * between the game canvas and the virtual game grid, the latter being set to fixed dimensions as in the 
	 * defines at the top of the GameGrid class.
	 * @param n_canvas_width The real width of the canvas in pixels.
	 * @param n_canvas_height The real height of the canvas in pixels.
	 */
	public static void setupGameGrid(int n_canvas_width, int n_canvas_height) {
		float f_canvas_width = n_canvas_width;
		canvas_width = n_canvas_width;
		float f_canvas_height = n_canvas_height;
		canvas_height = n_canvas_height;
		
		//Set scaling factor so that the gamegrid's real size is less than the total screen size
		scaling_factor = (f_canvas_width/GAMEGRID_WIDTH < f_canvas_height/GAMEGRID_HEIGHT) ? 
				(f_canvas_width/GAMEGRID_WIDTH) : (f_canvas_height/GAMEGRID_HEIGHT);
		Log.v("balls", "Scaling factor choice was " + scaling_factor);
				
		//we assume that the phone aspect ratio, in portrait mode, is greater than
		//160/140 (probably a decent assumption)
		//Thus we display the game "top-aligned"
		canvas_anchor_x = canvas_width / 2;
		//canvas_anchor_x = (int) ((((float) GAMEGRID_WIDTH/2) * scaling_factor));
		canvas_anchor_y = (int) ((((float) GAMEGRID_HEIGHT/2) * scaling_factor));
	}
	
	/** Translates from canvas X coordinates to game grid x coordinates
	 * 
	 * @param canvas_x The canvas x coordinate, in px.
	 * @return The virtual game grid coordinate corresponding to the canvas coordinate.
	 */
	public static int gameGridX(int canvas_x) {
		int retval = ( (int) (((float)canvas_x) / scaling_factor) );
		
		if(retval > GAMEGRID_WIDTH) return GAMEGRID_WIDTH;
		if(retval < 0) return(0);
		return(retval);
	}
	
	/** Translates from canvas Y coordinates to game grid y coordinates
	 * 
	 * @param canvas_y The canvas y coordinate, in px.
	 * @return The virtual game grid coordinate corresponding to the canvas coordinate.
	 */
	public static int gameGridY(int canvas_y) {
		int retval = ( (int) (((float)canvas_y) / scaling_factor) );
		
		if(retval > GAMEGRID_HEIGHT) return GAMEGRID_HEIGHT;
		if(retval < 0) return(0);
		return(retval);
	}
	
	/** Does the reverse of gameGridX(); translates from game grid coordinates to actual canvas
	 * pixel coordinates
	 * @param gamegrid_x The GameGrid virtual coordinate
	 * @return The real canvas coordinate represented by the game grid coordinate
	 */
	public static int canvasX(int gamegrid_x) {
		return( (int) (((float)gamegrid_x) * scaling_factor) );
	}
	
	/** Does the reverse of gameGridY(); translates from game grid coordinates to actual canvas
	 * pixel coordinates
	 * @param gamegrid_y The GameGrid virtual coordinate
	 * @return The real canvas coordinate represented by the game grid coordinate
	 */
	public static int canvasY(int gamegrid_y) {
		return( (int) (((float)gamegrid_y) * scaling_factor) );
	}
	
	/** Return the maximum canvas x dimensions that is mapped to this GameGrid. It is assumed that
	 * the minimum canvas x dimension is 0 (mapping to GameGrid 0)
	 *  
	 * @return The maximum canvas x coordinate that is mapped to within this GameGrid.
	 */
	public static int maxCanvasX() {
		return(canvas_anchor_x * 2);
	}
	
	/** Return the maximum canvas y dimensions that is mapped to this GameGrid. It is assumed that
	 * the minimum canvas y dimension is 0 (mapping to GameGrid 0)
	 *  
	 * @return The maximum canvas y coordinate that is mapped to within this GameGrid.
	 */
	public static int maxCanvasY() {
		return(canvas_anchor_y * 2);
	}
}
