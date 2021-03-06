/** Describe the "fast shoes" upgrade. See GameUpgrade class for details on what the 
 * class variables mean.  */

package org.coffeecats.coffeetime.gameobjects.upgradedefs;

import org.coffeecats.coffeetime.gameobjects.GameUpgrade;

public class FastShoesUpgrade extends GameUpgrade {
	
	public static final String UPGRADE_NAME = "fastshoes";

	public FastShoesUpgrade() {
		this.upgradeCost = 150;
		this.upgradeDescription = "Increase walking speed 10%";
		this.upgradeName = UPGRADE_NAME;
		this.upgradeLongName = "Fast Shoes";
		this.upgradeLevel = 0;
	}
}
