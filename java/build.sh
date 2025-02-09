mvn clean install 
pushd ./core
mvn clean install
popd
pushd ./spring
mv clean install
popd
pushd ./swing/beatsui
mvn clean install
popd
