## Docker Commands

### Preparing to build image

Copy all the files into a subdirectory of the directory containing the aem.dokerfile file called AemSoftware. 
This should include:

* the AEM GA Quickstart jar and license.properties
* the service pack files (service pack and forms-addon)
* the aem_cntrl jar
* (optionally) the fluent forms jars
* (optionally) an application.properties if you want to override any settings (like the trace level)

### Running container locally

* Commmand to build docker image

`docker buildx build --file aem.dockerfile -t aem:aem65sp21 .`

* Command to build and run docker container locally

`docker run -i -t -p 4502:4502 --name aem65sp21 aem:aem65sp21`

### Pushing to GitHub

* Login to GitHub Container Registry

`docker login --username your_user_name --password personal_github_token ghcr.io`

* Build the image with the correct tag

`docker buildx build --file aem.dockerfile -t ghcr.io/4pointsolutions-ps/aem:aem65sp21 .`

* Push to GitHub repo

`docker push ghcr.io/4pointsolutions-ps/aem:aem65sp21`
