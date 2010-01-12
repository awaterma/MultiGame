/*
* Copyright (C) 2008 ECOSUR, Andrew Waterman and Max Pimm
*
* Licensed under the Academic Free License v. 3.2.
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * @author awaterma@ecosur.mx
 */
package mx.ecosur.multigame.impl.entity.gente;

import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import mx.ecosur.multigame.impl.util.gente.GenteMoveComparator;
import org.drools.KnowledgeBase;
import org.drools.runtime.StatefulKnowledgeSession;

import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.impl.CellComparator;
import mx.ecosur.multigame.impl.Color;
import mx.ecosur.multigame.impl.DummyMessageSender;

import mx.ecosur.multigame.impl.enums.gente.GenteStrategy;

import mx.ecosur.multigame.impl.model.GridCell;
import mx.ecosur.multigame.impl.model.GridGame;
import mx.ecosur.multigame.impl.model.GridRegistrant;

import mx.ecosur.multigame.impl.enums.Direction;
import mx.ecosur.multigame.impl.util.Search;

import mx.ecosur.multigame.model.implementation.AgentImpl;
import mx.ecosur.multigame.model.implementation.GameImpl;

@Entity
public class GenteStrategyAgent extends GentePlayer implements AgentImpl {
	
	private static final long serialVersionUID = 6999849272112074624L;
	
	private GenteStrategy strategy;
	
	private GenteMove nextMove;
	
	public GenteStrategyAgent () {
		super();
		nextMove = null;
	}
	
	public GenteStrategyAgent (GridRegistrant player, Color color,
			GenteStrategy strategy) 
	{
		super (player, color);
		this.strategy = strategy;
	}
	
	/* (non-Javadoc)
	 * @see mx.ecosur.multigame.model.implementation.AgentImpl#initialize()
	 */
	public void initialize() {
		nextMove = null;
	}

	@Enumerated (EnumType.STRING)
	public GenteStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(GenteStrategy strategy) {
		this.strategy = strategy;
	}
	
	/**
	 * Returns a list of cells that are open and adjacent to cells of 
	 * the colors, "colors".
	 * 
	 * @param colors
	 * @return
	 */
	private TreeSet<GridCell> findUnboundAdjacentCells (GridGame game, HashSet<Color> colors) {
		TreeSet<GridCell> ret = new TreeSet<GridCell> (new CellComparator());
		TreeSet<GridCell> candidates = new TreeSet<GridCell> (new CellComparator());
		Search search = new Search (game.getGrid());
		
		/* Get all Cells of with the targeted Colors */
		for (GridCell cell : game.getGrid().getCells ()) {
			if (colors.contains(cell.getColor())) {
				candidates.add(cell);
			}
		}
		
		/* Do a search with a depth of 1 for all of those cells, if a null
		 * is returned, create a new cell and add it to the unbound list */
		for (GridCell candidate : candidates) {
			for (Direction direction : Direction.values()) {
				if (direction == Direction.UNKNOWN)
					continue;
				GridCell result = search.searchGrid(direction, candidate, 1);
				if (result == null) {
					ret.add(createDestination (direction, candidate, 1));
				}
			}
		}
		
		return ret;
	}
	
	public TreeSet<GenteMove> determineScoringMoves (GridGame game, Color color) throws InvalidMoveException {
		HashSet<Color> colors = new HashSet<Color> ();
		colors.add(color);
		return determineScoringMoves (game, colors);
	}
	
	/**
	 * Returns a list of cells that would result in a Tria or Tessera for the 
	 * colors, "colors".
	 * 
	 * @param colors
	 * @return
	 * @throws InvalidMoveException 
	 */
	public TreeSet<GenteMove> determineScoringMoves (GridGame game, HashSet<Color> colors) throws InvalidMoveException {
		TreeSet<GenteMove> ret = new TreeSet<GenteMove>(new GenteMoveComparator());
			/* Get the unbound adjacents, and speculate on moves */
		TreeSet<GridCell> unbound = this.findUnboundAdjacentCells(game, colors);
        boolean currentTurn = this.isTurn();
        
		for (GridCell cell : unbound){
            try {
                for (Color color : colors) {
                    this.setTurn(currentTurn);
                    GenteGame clone = (GenteGame) ((GenteGame) game).clone();
                    clone.setMessageSender(new DummyMessageSender());
                    /* Disable UI message recpetion */
                    clone.setId(0);
                    cell.setColor(color);
                    cell.setId(getMaxId(clone) + 1);
                    GenteMove move = new GenteMove (this, cell);
                    move = (GenteMove) clone.move(move);
                    if (move.getTesseras().size() >  0) {
                        ret.add(move);
                    } else if (move.getTrias().size() > 0) {
                        ret.add(move);
                    }
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        this.setTurn(currentTurn);
		return ret;
	}
	
	public TreeSet<GenteMove> determineAvailableMoves (GenteGame game) {
		return this.determineAvailableMoves (game, this.getColor());
	}
	
	public TreeSet<GenteMove> determineAvailableMoves (GridGame game, HashSet<Color> colors) {
		TreeSet<GenteMove> ret = new TreeSet<GenteMove>(new GenteMoveComparator());
		TreeSet<GridCell> unbound = this.findUnboundAdjacentCells(game, colors);
		for (GridCell cell : unbound){
			for (Color color : colors) {
				cell.setColor(color);
				ret.add(new GenteMove (this, cell));
			}
		}
		
		return ret;
	}
	
	public TreeSet<GenteMove> determineAvailableMoves (GenteGame game, Color color) {
		HashSet<Color> colors = new HashSet<Color> ();
		colors.add(color);
		return determineAvailableMoves (game, colors);
	}
	
	public HashSet<Color> oppositionColors () {
		HashSet <Color> ret = new HashSet<Color> ();
		HashSet<Color> teamColors = new HashSet<Color>();
		teamColors.add(getColor());
		teamColors.add (getColor().getCompliment());
		for (Color color : Color.values()) {
			if (!teamColors.contains(color))
				ret.add(color);
		}
		return ret;
	}

    /* (non-Javadoc)
      * @see mx.ecosur.multigame.Agent#determineNextMove(mx.ecosur.multigame.model.Game)
      */
    public GenteMove determineNextMove(GameImpl game) {
        if (isTurn()) {
            KnowledgeBase kBase = strategy.getRuleBase();
            StatefulKnowledgeSession session = kBase.newStatefulKnowledgeSession();
            session.insert(this);
            session.insert(game);
            session.insert(new DummyMessageSender());
            session.fireAllRules();
            session.dispose();
        }

        /* Rules should have created the next Move into this player, if
           * one available */
        GenteMove ret = getNextMove();
        if (ret != null) {
            GridCell destination = (GridCell) ret.getDestinationCell();
            destination.setColor(getColor());
            ret.setDestinationCell(destination);
            ret.setPlayer(this);
            if (!isTurn())
                throw new RuntimeException ("Move generated but agent has lost turn!");

        } else {
            if (isTurn()) {
                  throw new RuntimeException ("GenteStrategyAgent unable to find move during turn!");
            }
        }

        return ret;
    }
	
	public void setNextMove (GenteMove next) {
		this.nextMove = next;
	}
	
	@Transient
	public GenteMove getNextMove () {
		return nextMove;
	}

/**
	 * @param direction
	 * @param cell
     * @param factor
	 * @return
	 */
	private GridCell createDestination(Direction direction, GridCell cell, int factor) {
		int column = 0, row = 0;
		switch (direction) {
			case NORTH:
				column = cell.getColumn();
				row = cell.getRow() - factor;
				break;
			case SOUTH:
				column = cell.getColumn();
				row = cell.getRow() + factor;
				break;
			case EAST:
				column = cell.getColumn() + factor;
				row = cell.getRow();
				break;
			case WEST:
				column = cell.getColumn () - factor;
				row = cell.getRow();
				break;
			case NORTHEAST:
				column = cell.getColumn() + factor;
				row = cell.getRow() - factor;
				break;
			case NORTHWEST:
				column = cell.getColumn () - factor;
				row = cell.getRow() - factor;
				break;
			case SOUTHEAST:
				column = cell.getColumn() + factor;
				row = cell.getRow () + factor;
				break;
			case SOUTHWEST:
				column = cell.getColumn () - factor;
				row = cell.getRow() + factor;
				break;
			default:
				break;
		}

		return new GridCell (column, row, Color.UNKNOWN);
	}

    private int getMaxId (GenteGame game) {
        int max = 0;
        Set<GridCell> cells = game.getGrid().getCells();
        for (GridCell cell : cells) {
            if (cell.getId() > max)
                max = cell.getId();
        }

        return max;
    }
}
