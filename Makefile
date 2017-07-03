LIB_JARS=`find -L lib/ -name "*.jar" | tr "[:space:]" :`

compile:
	javac -sourcepath src -classpath $(LIB_JARS) `find src/ -L -name "*.java"`

jar:
	javac -classpath $(LIB_JARS) `find src -name "*.java"`
	cd src; jar -cf ../lib/bitpeer.jar `find peersim -name "*.class"`
	$(MAKE) clean

doc:
	mkdir -p doc
	javadoc -sourcepath src -classpath $(LIB_JARS) -d doc peersim.bittorrent

run:
	java -cp $(LIB_JARS):classes peersim.Simulator config-BitTorrent.cfg

all: compile doc run

clean: 
	rm -f `find src/peersim -name "*.class"`
