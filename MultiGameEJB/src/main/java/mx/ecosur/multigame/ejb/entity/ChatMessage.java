/*
* Copyright (C) 2008 ECOSUR, Andrew Waterman and Max Pimm
* 
* Licensed under the Academic Free License v. 3.0. 
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * A chat message sent across the system.
 * 
 * @author max@alwaysunny.com
 */

package mx.ecosur.multigame.ejb.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class ChatMessage implements Serializable {
	
	private static final long serialVersionUID = 444999377280040070L;

	//TODO: Chat messages should be related to moves
	
	/**
	 * Primary key
	 */
	private int id;
	
	/**
	 * The player that sent the message
	 */
	private GamePlayer sender;
		
	/**
	 * The time and date that the message was sent
	 */
	private Date dateSent;
	
	/**
	 * The body of the message 
	 */
	private String body;
	
	@Id @GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public GamePlayer getSender() {
		return sender;
	}

	public void setSender(GamePlayer sender) {
		this.sender = sender;
	}

	@Temporal(TemporalType.DATE)
	public Date getDateSent() {
		return dateSent;
	}

	public void setDateSent(Date dateSent) {
		this.dateSent = dateSent;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
