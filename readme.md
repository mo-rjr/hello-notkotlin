# hello-notkotlin

I wanted to get a Java app running as a custom runtime on AWS Lambda.
I decided to use as my starting point this guide, on which my code is heavily based: \
['Fighting cold startup issues for your Kotlin Lambda with GraalVM' by Mathias Düsterhöft](https://medium.com/@mathiasdpunkt/fighting-cold-startup-issues-for-your-kotlin-lambda-with-graalvm-39d19b297730) \
I also used the AWS custom runtime API definition: \
[AWS Lambda Runtime Interface](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html) \
which includes a link to a definition of the API in OpenAPI/Swagger format.  I also used \
['How to Deploy Java Application with Docker and GraalVM' by Vladimír Oraný](https://medium.com/agorapulse-stories/how-to-deploy-java-application-with-docker-and-graalvm-464629d95dbd) \
and various other web resources like the GraalVM docs and other AWS docs.  And lots of StackOverflow, obvs. 

It's called `hello-notkotlin` because it springs from the Kotlin article, 
but what the code actually does at heart is repeat a given String a given number of times, 
and it would more sensibly be called string-repeater or some such.

#### Note on Dockerfile
The Dockerfile ended up being a bit of a dead end because of my own dev environment, but I have left it in the code because it worked.  See docker.md

## The Java code
The Java code is run from `uk.co.littlestickyleaves.StringRepeatLambdaRunner`.  It fulfils the API contract by polling for input text, processing it, and posting back the result. 
It also catches and handles the two types of error: initialization errors, and processing errors.  \
The Java code is not intended as a general-purpose layer which can be used as a base for different lambdas, so it does not use reflection to create a Handler. 
The StringRepeater class which processes the input is also responsible for deserializing the Json. I've used the most lightweight version of Jackson.

I had a bit of trouble implementing the GETs and POSTs required by the API.  At first I didn't realise that GraalVM only goes up to Java 8, and tried to use the Java 11 HttpClient.
Then I tried to use Unirest, but something, possibly static initializers, annoyed GraalVM.  The Kotlin demo says that OkHttp isn't liked either. 
I started using Apache, but I worried it would have issues too, and by this point I was so bored with writing HTTP code that I just used
`java.net.HttpURLConnection` from Java 1.1 -- old school.  The required methods are defined in the `LambdaIOHandler` interface 
so it should be easy to swap in a better one.  (I think GraalVM can handle static stuff if warned about it, but I couldn't be bothered for this.)

## My dev context
My home machine is on Windows 10 Pro.  My work machine is too. 
I needed to use my home machine for this because I have more control over it: for example, I required a recent version of Maven. 
I have the Windows Subsystem for Linux on my home machine, but I usually develop in Windows on IntelliJ. 
I can run Docker from Windows, but not from WSL Ubuntu as yet.  But things like setting files to be executable required that I do some work at least from a Linux environment. \
At first I was doing quite well building using a Dockerfile in Windows, but the function didn't work, I think because it wasn't properly executable. \
In the end I used the WSL to make it work.  This means that a lot of my trouble and some of my instructions relate to WSL.  
In particular there was some fiddly stuff about windows vs linux PATH variables.  Luckily Ubuntu is pretty good at installing things for you.

Thing I had to have on WSL Ubuntu:
* java 8 not 11, jdk not just jre
* JAVA_HOME variable
* mvn
* aws cli
    * which in turn meant pip3, though I think it might have already been there
* git -- I found it easier to check out my own code to a specifically WSL place, rather than to try to use the version already on my Windows system
* GraalVM -- google how to get it, but essentially you download a tar.gz, unpack it, and add the bin to your PATH
* native-image -- you get this using GraalVM's gu tool: `gu install native-image`
* adding things to my `~/.bashrc` seemed to be the best way to deal with having persistent PATH variables, and if 
I added them to the front of the PATH it didn't matter if Windows variants of the same thing were found further down the list
(when stuff like java was only available in a Windows .exe form on the path I got confusing error messages)
* I needed some cpp header stuff to be available
    * online it suggested `sudo apt-get install build-essential` but I already seemed to have that
    * `sudo apt-get install zlib1g-dev` sorted me out
 
 ## The deployment package
 There needs to be a `bootstrap` file, as included in this repo.  All this does is start the native executable. 
 It's the file that the AWS system calls when using a provided runtime.  
 It has to be set to be executable, e.g. `chmod 777 bootstrap`.  
 
 To compile the code into a native executable using GraalVM, first compile it normally using maven. \
 `mvn clean package` \
 The `pom.xml` is set up to create a fat jar, as usual when building Java for AWS Lambdas. 
 It also puts the Main class into the Manifest file, but I don't think that was necessary in the long run 
 because you should specify this class in the native-image command. \
 Once you have the fat jar, use GraalVM's `native-image` command to create the native executable: \
 `native-image --enable-url-protocols=http -cp ./target/hello-notkotlin-1.0.jar \`  
  `-Djava.net.preferIPv4Stack=true \`  
  `-H:Name=notkotlin -H:Class=uk.co.littlestickyleaves.StringRepeatLambdaRunner -H:ReflectionConfigurationFiles=reflect.json \`  
  `-H:+ReportUnsupportedElementsAtRuntime --allow-incomplete-classpath`  
  This runs the native-image compiler, which is not snappy.  My understanding of it is only partial, but I think this is how it works:
  * you need the `--enable-url-protocols=http` bit because the executable will use http
  * the `-cp` bit tells it the classpath e.g. the jar you're using
  * you need the `-Djava.net.preferIPv4Stack=true` bit because without it there's an error which seems to be to do with IPv6
  * the Name and Class are pretty self-evident: Name is what the output gets called, and Class is the Main class of the app
  * you need the `-H:ReflectionConfigurationFiles=reflect.json` bit because otherwise GraalVM doesn't like reflection,
  and Jackson JR uses reflection for serialization and de- of output and in-
  * the last two bits are unclear to me but they seem to work, so *shrug*
  
Now you have the executable, if all has gone well. 
This is one of the major places where things failed when I was trying to make this work. 
For example, make sure you've got the cpp header stuff available (see the list of things you need in WSL Ubuntu, above). \
Make the executable executable (chmod etc).

Place the bootstrap file and the executable into a zip file together: \
`zip string-repeater.zip bootstrap notkotlin`  
This is your deployment package!

## Making a function on AWS using the deployment package
Obviously you now need the aws cli.  If you haven't already configured it, here are some easy instructions: \
[Quickly Configuring the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html#cli-quick-configuration) \
You'll also need a role for your lambda.  It has to have `AWSLambdaBasicExecutionRole` permissions: \
[AWS Lambda Execution Role](https://docs.aws.amazon.com/lambda/latest/dg/lambda-intro-execution-role.html) \
Take note of the role's arn so you can put it into your aws cli command.

Now try this command to make the function: \
`aws lambda create-function --function-name repeat-string --zip-file fileb://~/linuxWorkarea/hello-notkotlin/string-repeater.zip --handler function.handler --runtime provided --role arn:aws:iam::`YOUR_ARN_HERE \
The zip file has to be referenced with the `fileb://` protocol.  \
If this is not your first attempt you'll need to delete the previous version: \
`aws lambda delete-function --function-name repeat-string` \
You should get back some info about the created function. 

Now you can try to invoke the function on AWS like this: \
`aws lambda invoke --function-name repeat-string --payload '{"input":"beep","repeat":4}' response.txt`\
(Don't put any spaces into your json payload.)
The response to the cli command should give you some idea if it has worked or not. 
It will have put the proper response into `response.txt`, and this will either be more detail on the error, or the correct output.   
Here is a response.txt contents from a successful invocation: \
`{"input":"neenah","repeat":9,"result":"neenahneenahneenahneenahneenahneenahneenahneenahneenah"}` \
If it has failed, more detail can be got from looking at the CloudWatch logs in the AWS console.  The error message may be somewhat opaque.

If you have got this far, mazel tov!  I'm writing these instructions after the event, 
using the notes I made over the long process of trying to get it working. 
There were a ton of dead ends and blind alleys, so I may well have left out something vital 
which seemed like a byway at the time, but I've done my best to capture what I think worked in the end. 
Feedback appreciated.
