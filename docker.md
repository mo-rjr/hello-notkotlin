# Dockerfile

This worked fine (using Docker for Windows) though I didn't use it in the end.

I had a total pain trying to get the compiled GraalVM executable out of the Docker container.

If you build the Docker file, it's best to give it a tag, e.g. \
`docker build -t hello-notkotlin .`

Then this command will give you the id of the container: \
`docker run -d --name try hello-notkotlin` \
and also give it the name `try`.

Then you can run this command, where the long number is the id of the container: \
`docker cp 6662b0b6a687533116899d50fbdd82c96e33573247cc081c823ecc738c823e0d:notkotlin /c/Workarea/hello-notkotlin/target`

