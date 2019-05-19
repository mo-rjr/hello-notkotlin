# hello-notkotlin

This is prompted by slow reading of:

['Fighting cold startup issues for your Kotlin Lambda with GraalVM' by Mathias Düsterhöft](https://medium.com/@mathiasdpunkt/fighting-cold-startup-issues-for-your-kotlin-lambda-with-graalvm-39d19b297730)

It is an attempt to make a simple GraalVM custom runtime for a Java AWS Lambda

`Dockerfile` comes from ['How to Deploy Java Application with Docker and GraalVM' by Vladimír Oraný](https://medium.com/agorapulse-stories/how-to-deploy-java-application-with-docker-and-graalvm-464629d95dbd)

I had a total pain trying to get the compiled GraalVM executable out of the Docker container.

If you build the Docker file, it's best to give it a tag, e.g. \
`docker build -t hello-notkotlin .`

Then this command will give you the id of the container: \
`docker run -d --name try hello-notkotlin`
and also give it the name `try`.

Then you can run this command, where the long number is the id of the container: \
`docker cp 6662b0b6a687533116899d50fbdd82c96e33573247cc081c823ecc738c823e0d:notkotlin /c/Workarea/hello-notkotlin/target`

Having got this far I am too hungry to try it out on Lambda.

