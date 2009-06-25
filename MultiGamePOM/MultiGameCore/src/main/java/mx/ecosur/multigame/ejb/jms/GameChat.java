/*
* Copyright (C) 2008 ECOSUR, Andrew Waterman and Max Pimm
* 
* Licensed under the Academic Free License v. 3.0. 
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * Receives messages from the JMS queue, and adds ChatMessages into the 
 * system through the SharedBoard. 
 * 
 * @author max@alwayssunny.com
 */

package mx.ecosur.multigame.ejb.jms;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import mx.ecosur.multigame.GameEvent;
import mx.ecosur.multigame.ejb.SharedBoardLocal;
import mx.ecosur.multigame.model.ChatMessage;

@MessageDriven(mappedName = "MultiGame")
public class GameChat implements MessageListener {

	private static Logger logger = Logger.getLogger(GameChat.class
			.getCanonicalName());
	@EJB
	private SharedBoardLocal sharedBoard;

	/**
	 * Simple onMessage method appends the name of the Player that sent a
	 * message, and the messages contents to the SharedBoard stream
	 */
	public void onMessage(Message message) {

		ObjectMessage msg = (ObjectMessage) message;
		GameEvent gameEvent;
		try {
			// TODO: Add selector or filter to only treat CHAT messages
			gameEvent = GameEvent.valueOf(msg.getStringProperty("GAME_EVENT"));
			if (gameEvent.equals(GameEvent.CHAT)) {
				ChatMessage chatMessage = (ChatMessage) msg.getObject();
				sharedBoard.addMessage(chatMessage);
				msg.acknowledge();
			}
		} catch (JMSException e) {
			logger.warning("Not able to save game message: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
