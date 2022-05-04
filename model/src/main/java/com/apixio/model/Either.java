package com.apixio.model;

public abstract class Either<A, B> {
	public A left() { return null; }
	public B right() { return null; }
	
	/**
	 * The left hand type of the union type
	 * @author vvyas
	 *
	 * @param <A>
	 * @param <B>
	 */
	public static class Left<A,B> extends Either<A,B> {
		A left;	
		public Left(A a) { this.left = a; }		
		@Override
		public A left() { return left; }
	}
	
	/**
	 * The right hand type of the union type 
	 * @author vvyas
	 *
	 * @param <A>
	 * @param <B>
	 */
	public static class Right<A,B> extends Either<A,B> {
		B right;
		public Right(B b) { this.right = b; }
		
		@Override
		public B right() { return right; }		
	}
}
