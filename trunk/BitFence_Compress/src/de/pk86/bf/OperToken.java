package de.pk86.bf;

import de.pk86.bf.pl.Slot;

public class OperToken {
	public String token;
	public int oper;
	public Slot slot;
	
	public OperToken(String token, int oper) {
		this.token = token;
		this.oper = oper;
	}

}
