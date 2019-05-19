# use graalvm image
FROM oracle/graalvm-ce:1.0.0-rc16

# copy the fat jar
COPY target/hello-notkotlin-1.0.jar hello-notkotlin.jar

# copy the reflection file
COPY reflect.json reflect.json

# run the native image compiler
RUN native-image --enable-url-protocols=http \
    -Djava.net.preferIPv4Stack=true \
    -H:ReflectionConfigurationFiles=reflect.json \
    -H:+ReportUnsupportedElementsAtRuntime \
    --no-server \
    --class-path hello-notkotlin.jar \
    -H:Name=notkotlin -H:Class=uk.co.littlestickyleaves.StringRepeatLambdaRunner \
    -H:+AllowVMInspection

    ## but now I don't know how to get it out again!
