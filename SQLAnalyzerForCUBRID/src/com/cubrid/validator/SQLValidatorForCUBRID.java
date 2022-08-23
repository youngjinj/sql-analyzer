package com.cubrid.validator;

public class SQLValidatorForCUBRID {

	static {
		try {
			/* -Djava.library.path=jni */
			System.loadLibrary("sqlvalidator");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}

	public native String validateSQL(String query);

	public static void main(String[] args) {
		SQLValidatorForCUBRID jniExample = new SQLValidatorForCUBRID();
		String result = jniExample.validateSQL("select * from game;");
		System.err.println(result);
	}
}
