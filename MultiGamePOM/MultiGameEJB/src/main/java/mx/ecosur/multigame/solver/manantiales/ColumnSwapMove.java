/*
* Copyright (C) 2008, 2009 ECOSUR, Andrew Waterman
*
* Licensed under the Academic Free License v. 3.2.
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * @author awaterma@ecosur.mx
 */
package mx.ecosur.multigame.solver.manantiales;

import mx.ecosur.multigame.ejb.entity.manantiales.Token;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.drools.FactHandle;
import org.drools.WorkingMemory;
import org.drools.solver.core.move.Move;

public class ColumnSwapMove implements Move {
	
	Token token, swapToken;
	int toColumn;
	
	public ColumnSwapMove (Token token, Token swapToken, int toColumn) {
		this.token = token;
		this.swapToken = swapToken;
		this.toColumn = toColumn;
	}

	/* (non-Javadoc)
	 * @see org.drools.solver.core.move.Move#createUndoMove(org.drools.WorkingMemory)
	 */
	public Move createUndoMove(WorkingMemory workingMemory) {
		return new ColumnSwapMove (swapToken, token, token.getColumn());
	}

	/* (non-Javadoc)
	 * @see org.drools.solver.core.move.Move#doMove(org.drools.WorkingMemory)
	 */
	public void doMove(WorkingMemory wm) {
		FactHandle tokenHandle = wm.getFactHandle(token);
		FactHandle swapHandle = wm.getFactHandle(swapToken);
		
		Token newToken = new Token (toColumn, token.getRow(), token.getColor(),
				token.getType());
		Token swappedToken = new Token (token.getColumn(), token.getRow(), token.getColor(),
				swapToken.getType());
		
		/* Update WorkingMemory in one block */
		synchronized (wm) {
			wm.modifyRetract(tokenHandle);
			wm.modifyInsert(tokenHandle, newToken);
			wm.modifyRetract (swapHandle);
			wm.modifyInsert (swapHandle, swappedToken);
		}		
	}

	/* (non-Javadoc)
	 * @see org.drools.solver.core.move.Move#isMoveDoable(org.drools.WorkingMemory)
	 */
	public boolean isMoveDoable(WorkingMemory wm) {
		return (token.getColumn() != toColumn &&
				!token.equals(swapToken));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "swap " + this.token + " => " + swapToken;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof ColumnSwapMove) {
			ColumnSwapMove comparison = (ColumnSwapMove) obj;
				/* Moves are equal if it is the same change */
			if (comparison.toColumn == this.toColumn && comparison.token.equals(this.token))
				ret = true;
		} else {
			ret = super.equals(obj);
		}
		
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder() 
        .append(token) 
        .append(swapToken) 
        .toHashCode(); 
	}
}
