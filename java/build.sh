pushd ~/dev/BeatGeneratorApp/java

find . -iname target -type d -exec rm -rfv {} \;

mvn clean -N
mvn dependency:resolve

pushd ./core
mvn clean install
popd

mvn clean install 

pushd ./spring
mv clean install
popd

pushd ./swing/beatsui
mvn clean install
popd

popd

