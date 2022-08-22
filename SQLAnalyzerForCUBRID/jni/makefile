# Define a variable for classpath
CLASS_PATH = ../target/classes/

# Define a variable for jdk_path
JDK_PATH = C:\DATA\Development\java-1.8.0-openjdk-1.8.0.275-1.b01.ojdkbuild.windows.x86_64

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : sqlvalidator.dll

# $@ matches the target, $< matches the first dependancy
sqlvalidator.dll : SQLValidatorForCUBRID.o
	gcc -shared -o $@ $<

# $@ matches the target, $< matches the first dependancy
SQLValidatorForCUBRID.o : SQLValidatorForCUBRID.c SQLValidatorForCUBRID.h
	gcc -m64 -g -I"$(JDK_PATH)\include" -I"$(JDK_PATH)\include\win32" -c $< -o $@

# $* matches the target filename without the extension
SQLValidatorForCUBRID.h : com/cubrid/validator/SQLValidatorForCUBRID.class
	javah -classpath $(CLASS_PATH) com.cubrid.validator.$*

clean :
	rm com_cubrid_validator_SQLValidatorForCUBRID.h SQLValidatorForCUBRID.o sqlvalidator.dll