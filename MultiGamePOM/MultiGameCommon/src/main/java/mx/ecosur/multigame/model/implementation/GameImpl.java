/*
* Copyright (C) 2008, 2009 ECOSUR, Andrew Waterman
*
* Licensed under the Academic Free License v. 3.2.
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * @author awaterma@ecosur.mx
 */
package mx.ecosur.multigame.model.implementation;

import java.util.Collection;
import java.util.List;

import mx.ecosur.multigame.enums.GameState;
import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.model.GamePlayer;

public interface GameImpl extends Implementation {

	/**
	 * @return
	 */
	public int getId();

	/**
	 * @param registrant
	 * @return
	 */
	public GamePlayerImpl registerPlayer(RegistrantImpl registrant);

	/**
	 * @param player
	 */
	public void removePlayer(GamePlayerImpl player);

	/**
	 * @param state
	 */
	public void setState(GameState state);

	/**
	 * @return
	 */
	public GameState getState();

	/**
	 * @param move
	 */
	public void move(MoveImpl move) throws InvalidMoveException;

	/**
	 * @return
	 */
	public Collection<MoveImpl> getMoves();

	/**
	 * @return
	 */
	public List<GamePlayer> listPlayers();

	/**
	 * @return
	 */
	public int getMaxPlayers();

}
