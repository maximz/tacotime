package com.yulaev.tacotime.gameobjects.upgradedefs;

import com.yulaev.tacotime.gameobjects.GameUpgrade;

public class CoffeeMachineUpgrade extends GameUpgrade {
	
	public static final String UPGRADE_NAME = "secondcoffeemachine";

	public CoffeeMachineUpgrade() {
		this.upgradeCost = 30;
		this.upgradeDescription = "Adds a second coffee machine to the cafe";
		this.upgradeName = UPGRADE_NAME;
		this.upgradeLongName = "2nd Coffee Machine";
		this.upgradeLevel = 0;
	}
}
