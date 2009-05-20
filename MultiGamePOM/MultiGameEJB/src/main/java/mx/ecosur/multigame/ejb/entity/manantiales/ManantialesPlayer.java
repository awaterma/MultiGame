/*
* Copyright (C) 2008, 2009 ECOSUR, Andrew Waterman
*
* Licensed under the Academic Free License v. 3.2.
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * @author awaterma@ecosur.mx
 */
package mx.ecosur.multigame.ejb.entity.manantiales;

import javax.persistence.Entity;

import mx.ecosur.multigame.Color;
import mx.ecosur.multigame.ejb.entity.Game;
import mx.ecosur.multigame.ejb.entity.GamePlayer;
import mx.ecosur.multigame.ejb.entity.Player;

@Entity
public class ManantialesPlayer extends GamePlayer {
	
	private static final long serialVersionUID = 2815240265995571202L;
	
	private int points, forested, moderate, intensive, vivero, silvo;
	
	private boolean badYear;
	
	public ManantialesPlayer () {
		super();
		badYear = false;
	}
	
	public ManantialesPlayer (Game game, Player player, Color favoriteColor) {
		super (game, player, favoriteColor);
		points = 0;
		badYear = false;	
	}

	public int getScore() {
		return points;
	}
	
	public void setScore (int score) {
		this.points = score;
	}

	/**
	 * @return the forested
	 */
	public int getForested() {
		return forested;
	}

	/**
	 * @return the moderate
	 */
	public int getModerate() {
		return moderate;
	}

	/**
	 * @return the intensive
	 */
	public int getIntensive() {
		return intensive;
	}

	/**
	 * @return the vivero
	 */
	public int getVivero() {
		return vivero;
	}

	/**
	 * @return the silvo
	 */
	public int getSilvo() {
		return silvo;
	}

	/**
	 * @param forested the forested to set
	 */
	public void setForested(int forested) {
		this.forested = forested;
	}

	/**
	 * @param moderate the moderate to set
	 */
	public void setModerate(int moderate) {
		this.moderate = moderate;
	}

	/**
	 * @param intensive the intensive to set
	 */
	public void setIntensive(int intensive) {
		this.intensive = intensive;
	}

	/**
	 * @param vivero the vivero to set
	 */
	public void setVivero(int vivero) {
		this.vivero = vivero;
	}

	/**
	 * @param silvo the silvo to set
	 */
	public void setSilvo(int silvo) {
		this.silvo = silvo;
	}

}
