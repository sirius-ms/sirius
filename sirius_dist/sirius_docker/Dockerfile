FROM ubuntu:22.04

RUN apt-get update &&\
	apt-get upgrade -y &&\
	apt-get install -y liblapack-dev
	
RUN mkdir -p app
WORKDIR /app

# copy sirius from workflow
COPY ./sirius-artifact/ .
	
# ensure sirius can be run
RUN chmod +x ./sirius/bin/sirius

# add the sirius directory to the PATH
ENV PATH="/app/sirius/bin:${PATH}"

# print sirius help on 'run' without arguments
CMD sirius --help

# change working directory to root
WORKDIR /
