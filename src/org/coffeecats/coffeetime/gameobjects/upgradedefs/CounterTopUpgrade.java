/** Describe the CounterTop upgrade. See CounterTop (GameItem) class for details on what GameItem
 * this upgrade introduces into the game. See GameUpgrade class for details on what the 
 * class variables mean. 
 */

package org.coffeecats.coffeetime.gameobjects.upgradedefs;

import org.coffeecats.coffeetime.gameobjects.GameUpgrade;

public class CounterTopUpgrade extends GameUpgrade {
	
	public static final String UPGRADE_NAME = "countertop";

	public CounterTopUpgrade() {
		this.upgradeCost = 100;
		this.upgradeDescription = "Add a counter top that can store an item temporarily";
		this.upgradeName = UPGRADE_NAME;
		this.upgradeLongName = "Counter Top";
		this.upgradeLevel = 0;
	}
}
