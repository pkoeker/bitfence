package de.pk86.bf;

import de.pk86.bf.pl.Slot;

public class OperToken {
	public static enum Brace {OPEN,NONE,CLOSE};
	public String token;
	public int oper;
	public Brace brace = Brace.NONE;
	public int level; // Klammer-level
	public Slot slot;
	
	public OperToken(String token, int oper) {
		this.token = token;
		this.oper = oper;
	}
	
	public String toString() {
		String ret = "";
		switch (brace) {
		case OPEN:
			ret = "(" + token;
			break;
		case CLOSE:
			ret = token + ")";
			break;
		default:
			ret = token;
		}
		return ret;
	}

}
