### AutoScout24ListingReport

This Uses a couple of csv files to generate a report which is available in tabular html format and JSON format

##### Milestones

1. Write a web application that displays / outputs the reports. This can be server generated html with no styling or some form of api depending on your preferences.
    > to test this visit: `localhost:9000/` it will render a minimaly styled html markup to render the reports
   
2. The project manager reaches out to you and mentions that they will want regularly to provide new input files
    - Add an upload endpoint to the server that receives CSV files, validates their format and uses the data in the uploaded CSV to fulfill the above requirements for subsequent requests
      
      > To Upload new files make a multipart form data POST request to `localhost:9000/` with the desired file. 
      > either the listing.csv or contacts.csv can be uploaded using the same form.
   
3. Engineers from another team reach out and would like to re-use your aggregations. Add an api endpoint which exposes the data in a structured format.
    > to get the Json formatted report, make a GET request to `localhost:9000/api`
    

##### How to run

I will assume Scala > 2.10 and sbt is installed on the machine.

Clone repository:
>  `git clone git@github.com:horlahlekhon/AutoScout24ListingReport.git`

Run test:
> `cd AutoScout24ListingReport`
> `sbt test` this will run the tests

Run Server:
> `cd AutoScout24ListingReport`
> `sbt run` this will start the server and listens at port 9000

      
