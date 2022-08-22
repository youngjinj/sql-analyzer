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

	public native void validateSQL(String query);

	public static void main(String[] args) {
		SQLValidatorForCUBRID jniExample = new SQLValidatorForCUBRID();
		jniExample.validateSQL("select 1");
	}
}
