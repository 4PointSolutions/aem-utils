## Docker Commands

### Preparing to build image

Copy all the installation files into a subdirectory called AemSoftware below the directory containing the appropriate aem.dockerfile files. 

The installation files in AemSoftware should include:
* the AEM GA Quickstart jar and license.properties
* (optionally) any service pack jar files 
* the forms add-on jar file
* the aem_cntrl jar
* (optionally) the fluent forms jars
* (optionally) an application.properties if you want to override any settings (like the trace level)

If using the AEM LTS, alter the `aem_lts.dockerfile` CMD line (at the end of the file) to launch the correct crx quickstart jar (e.g. `crx-quickstart/app/cq-quickstart-6.6.1-standalone-quickstart.jar` for 6.5 LTS SP1).

### Running container locally

* Commmand to build docker image

`docker buildx build --file aem_65.dockerfile -t aem:aem65sp21 .`

or

`docker buildx build --file aem_lts.dockerfile -t aem:aem65lts .`

* Command to build and run docker container locally

`docker run -i -t -p 4502:4502 --name aem65sp21 aem:aem65sp21`

or

`docker run -i -t -p 4502:4502 --name aem65lts aem:aem65lts`

### Pushing to GitHub

* Login to GitHub Container Registry

`docker login --username your_user_name --password personal_github_token ghcr.io`

* Build the image with the correct tag

`docker buildx build --file aem.dockerfile -t ghcr.io/4pointsolutions-ps/aem:aem65sp21 .`

* Push to GitHub repo

`docker push ghcr.io/4pointsolutions-ps/aem:aem65sp21`
