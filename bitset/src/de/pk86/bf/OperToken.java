package de.pk86.bf;

import de.pk86.bf.pl.Item;

public class OperToken {
	public static enum Brace {OPEN,NONE,CLOSE};
	
	public String token;
	public Selection.Oper oper;
	public Brace brace = Brace.NONE;
	public int level; // Klammer-level
	public Item slot;
	
	public OperToken(String token, Selection.Oper oper) {
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
