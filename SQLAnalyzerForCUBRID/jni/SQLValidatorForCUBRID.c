/* DO NOT EDIT THIS FILE - it is machine generated */
#include "com_cubrid_validator_SQLValidatorForCUBRID.h"

#include <jni.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>

/* Header for class com_cubrid_validator_SQLValidatorForCUBRID */

/*
 * Class:     com_cubrid_validator_SQLValidatorForCUBRID
 * Method:    validateSQL
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_cubrid_validator_SQLValidatorForCUBRID_validateSQL(
		JNIEnv *env, jobject jObj, jstring jStr) {
	const char *buf = NULL;
	char *query = NULL;
	int error = 0;

	buf = (*env)->GetStringUTFChars(env, jStr, NULL);
	query = strdup(buf);
	(*env)->ReleaseStringUTFChars(env, jStr, buf);
	printf("Query: %s\n", query);

	printf("hi, youngjinj. %s\n", query);
}
