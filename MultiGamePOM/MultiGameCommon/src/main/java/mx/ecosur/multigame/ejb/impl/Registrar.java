/*
* Copyright (C) 2008 ECOSUR, Andrew Waterman and Max Pimm
* 
* Licensed under the Academic Free License v. 3.0. 
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * Registration is the process of adding or finding users in the system and
 * associating that user with a current or new game.  Ficha colors are 
 * determined dynamically, by the available colors per game.
 * 
 * @author awaterma@ecosur.mx
 */

package mx.ecosur.multigame.ejb.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import mx.ecosur.multigame.MessageSender;
import mx.ecosur.multigame.ejb.interfaces.RegistrarRemote;
import mx.ecosur.multigame.ejb.interfaces.RegistrarLocal;
import mx.ecosur.multigame.enums.GameState;

import mx.ecosur.multigame.exception.InvalidRegistrationException;
import mx.ecosur.multigame.model.Agent;
import mx.ecosur.multigame.model.Game;
import mx.ecosur.multigame.model.GamePlayer;
import mx.ecosur.multigame.model.Registrant;
import mx.ecosur.multigame.model.implementation.RegistrantImpl;

@WebService 
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class Registrar implements RegistrarRemote, RegistrarLocal {

	private MessageSender messageSender;
	
	@PersistenceContext (unitName="MultiGame")
	EntityManager em;

	/**
	 * Default constructor
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Registrar() throws InstantiationException, IllegalAccessException, 
		ClassNotFoundException 
	{
		super();
		messageSender = new MessageSender();
	}
	
	/* (non-Javadoc)
	 * @see mx.ecosur.multigame.ejb.interfaces.RegistrarInterface#register(java.lang.String)
	 */
	@WebMethod (operationName = "Register")
	public Registrant register(Registrant registrant) {
		Registrant ret = null;
		RegistrantImpl impl = registrant.getImplementation();
		
		/* TODO: inject or make this query static */
		Query query = em.createNamedQuery("getRegistrantByName");
		query.setParameter("name", impl.getName());
		List<RegistrantImpl> registrants = query.getResultList();
		if (registrants.size() == 0) { 
			em.persist(impl);
			ret = new Registrant (impl);
		} else {			
			RegistrantImpl reg = (RegistrantImpl) registrants.get(0);
			ret = new Registrant (reg);
		}
		
		return ret;
	}		
	
	/**
	 * Registers a robot with she specified Game object.
	 * 
	 * TODO:  Make this generic.
	 * @throws InvalidRegistrationException 
	 */
	@WebMethod (operationName = "RegisterPlayer") 
	public GamePlayer registerPlayer (Game game, Registrant registrant) 
		throws InvalidRegistrationException 
	{
		if (!em.contains(game.getImplementation())) {
			Game test = new Game(em.find(game.getImplementation().getClass(), game.getId()));
			if (test.getImplementation() == null) 
				em.persist(game.getImplementation());
			else
				game = new Game (test.getImplementation());
		}
		
		if (!em.contains(registrant.getImplementation())) {
			Registrant test = new Registrant (em.find (
					registrant.getImplementation().getClass(), registrant.getId()));
			if (test.getImplementation() == null)
				em.persist(registrant.getImplementation());
			else
				registrant = new Registrant (test.getImplementation());
		}				

		registrant.setLastRegistration(System.currentTimeMillis());
		GamePlayer player = game.registerPlayer (registrant);		
		em.persist(player.getImplementation());			
		messageSender.sendPlayerChange(game);		
		return player;	
	}
	

	/* (non-Javadoc)
	 * @see mx.ecosur.multigame.ejb.interfaces.RegistrarInterface#registerAgent(mx.ecosur.multigame.model.Game, mx.ecosur.multigame.model.Agent)
	 */
	@WebMethod (operationName = "RegisterAgent")
	public Agent registerAgent(Game game, Agent agent) throws 
		InvalidRegistrationException 
	{		
		if (!em.contains(game.getImplementation())) {
			Game test = new Game(em.find(game.getImplementation().getClass(), game.getId()));
			if (test.getImplementation() == null) 
				em.persist(game.getImplementation());
			else
				game = new Game (test.getImplementation());
		}
		
		agent.setGame(game);
		
		if (!em.contains(agent.getImplementation())) {
			Agent test = new Agent (em.find (
					agent.getImplementation().getClass(), agent.getId()));
			if (test.getImplementation() == null)
				em.persist(agent.getImplementation());
			else
				agent = new Agent (test.getImplementation());
		}		
		
		agent = game.registerAgent (agent);
		
		/* Merge changes*/
		em.merge(agent.getImplementation());
		em.merge(game.getImplementation());	
		
		messageSender.sendPlayerChange(game);		
		return agent;		
	}		

	@WebMethod (operationName = "Unregister")
	public void unregister(Game game, GamePlayer player) 
		throws 
	InvalidRegistrationException {

		/* Remove the user from the Game */
		if (!em.contains(game.getImplementation()))
			game = new Game (em.find(game.getImplementation().getClass(), game.getId()));

		/* refresh the game object */
		em.refresh (game.getImplementation());
		game.removePlayer(player);
		game.setState(GameState.ENDED);
	}

	/* (non-Javadoc)
	 * @see mx.ecosur.multigame.ejb.RegistrarInterface#getUnfinishedGames(mx.ecosur.multigame.model.Registrant)
	 */
	public List<Game> getUnfinishedGames(Registrant player) {
		return player.getCurrentGames(em);		
	}

	/* (non-Javadoc)
	 * @see mx.ecosur.multigame.ejb.RegistrarInterface#getPendingGames(mx.ecosur.multigame.model.Registrant)
	 */
	public List<Game> getPendingGames(Registrant player) {
		return player.getAvailableGames(em);
	}
}
