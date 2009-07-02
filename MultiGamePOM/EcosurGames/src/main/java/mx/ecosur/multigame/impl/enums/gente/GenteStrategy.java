/*
* Copyright (C) 2008 ECOSUR, Andrew Waterman and Max Pimm
* 
* Licensed under the Academic Free License v. 3.0. 
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
 * Enum for articulating playing strategies and rulesbases for those
 * strategies.
 * 
 * @author awaterma@ecosur.mx
 */

package mx.ecosur.multigame.impl.enums.gente;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.compiler.DroolsParserException;
import org.drools.compiler.PackageBuilder;

public enum GenteStrategy {
	
	RANDOM, BLOCKER, SIMPLE;
	
	private RuleBase ruleBase;
	
	private static Logger logger = Logger.getLogger(GenteStrategy.class
			.getCanonicalName());

	public RuleBase getRuleBase() {
		/* Check that rule set has not already been created */
		if (ruleBase != null) {
			return ruleBase;
		}

		try {

			logger.fine("Initializing rule set for type " + this);

			/* Initialize the rules based on the type of game */
			PackageBuilder builder = new PackageBuilder();
			InputStreamReader reader = null;

			switch (this) {
			case BLOCKER:
				reader = new InputStreamReader(this.getClass()
						.getResourceAsStream(
								"/mx/ecosur/multigame/impl/agent/blocker-agent.drl"));
				builder.addPackageFromDrl(reader);
				break;
			case RANDOM:
				reader = new InputStreamReader(this.getClass()
						.getResourceAsStream(
								"/mx/ecosur/multigame/impl/agent/random-agent.drl"));
				builder.addPackageFromDrl(reader);
				break;
			case SIMPLE:
				reader = new InputStreamReader(this.getClass()
						.getResourceAsStream(
								"/mx/ecosur/multigame/impl/agent/simple-agent.drl"));
				builder.addPackageFromDrl(reader);
				break;					
			default:
				break;
			}

			if (reader != null)
				reader.close();

			/* Create the ruleBase */
			ruleBase = RuleBaseFactory.newRuleBase();
			ruleBase = RuleBaseFactory.newRuleBase();
			ruleBase.addPackage(builder.getPackage());

			logger.fine("Rule set for type " + this + " added to rulesets.");
			return ruleBase;

		} catch (DroolsParserException e) {
			e.printStackTrace();
			throw new RuntimeException (e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
}
