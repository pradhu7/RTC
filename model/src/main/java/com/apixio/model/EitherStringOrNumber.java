package com.apixio.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;



public class EitherStringOrNumber extends Either<String, Number> {
	private String s;
	private Number n;
	
	@Override
	public String left() { return s; }
	
	@Override 
	public Number right() { return n; }
	
	public EitherStringOrNumber(String s) {
		this.s = s;
		this.n = null;
	}
	
	public EitherStringOrNumber(Number n) {
		this.n = n;
		this.s = null;
	}
	
	public static class EitherString extends EitherStringOrNumber {
		public EitherString(String a) {
			super(a);
		}		
	}
	
	public static class EitherNumber extends EitherStringOrNumber {
		public EitherNumber(Number b) {
			super(b);
		}
		
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.n).append(this.s).appendSuper(super.hashCode()).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof EitherStringOrNumber){
			EitherStringOrNumber s = (EitherStringOrNumber) obj;
			return new EqualsBuilder().append(this.n, s.n).append(this.s, s.s).appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
	@Override
	public String toString() {
		if (s != null)
			return s;
		else if (n != null)
			return String.valueOf(n);
		return null;
	}
	
	
}
