package org.ggp.base.player.request.grammar;

import org.ggp.base.player.gamer.exception.AbortingException;

public abstract class Request
{

	public abstract String process(long receptionTime) throws AbortingException;

	public abstract String getMatchId();

	@Override
	public abstract String toString();

}
