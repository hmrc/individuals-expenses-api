Individuals Expenses API
========================
These API endpoints allow software packages to retrieve, amend and delete expenses for:

·       income for trade union and patent royalties

·       existing employment expense

An endpoint also exists that allows software packages to ignore HMRC provided employment expenses for a user.
    
## Requirements
- Scala 2.12.x
- Java 8
- sbt 1.3.7
- [Service Manager](https://github.com/hmrc/service-manager)
    
## Development Setup

Run from the console using: `sbt run` (starts on port 7795 by default)

Start the service manager profile: `sm --start MTDFB_SA`
 
## Run tests
```
sbt test
sbt it:test
```

## To view the RAML

To view documentation locally ensure the Expenses API is running, and run api-documentation-frontend:
`./run_local_with_dependencies.sh`

Then go to http://localhost:9680/api-documentation/docs/api/preview and use this port and version:
`http://localhost:7795/api/conf/1.0/application.raml`

## Reporting Issues

You can create a GitHub issue [here](https://github.com/hmrc/individuals-expenses-api/issues)


## API Reference / Documentation 
Available on the [Individuals Expenses Documentation](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-expenses-api/1.0)


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
