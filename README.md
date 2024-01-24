Individuals Expenses API
========================
These API endpoints allow software packages to retrieve, amend and delete expenses for:

* income for trade union and patent royalties
* existing employment expense

An endpoint also exists that allows software packages to ignore HMRC provided employment expenses for a user.

## Requirements

- Scala 2.13.x
- Java 11
- sbt 1.9.x
- [Service Manager V2](https://github.com/hmrc/sm2)

## Development Setup

Run from the console using: `sbt run` (starts on port 7795 by default)

Start the service manager profile: `sm2 --start MTDFB_INDIVIDUALS_EXPENSES`

## Running tests

```
sbt test
sbt it:test
```

## Viewing OAS

To view documentation locally ensure the Expenses API is running, and run api-documentation-frontend:
`./run_local_with_dependencies.sh`

Then go to http://localhost:9680/api-documentation/docs/openapi/preview and use this port and version:
`http://localhost:7795/api/conf/2.0/application.yaml`

## Changelog

You can see our changelog [here](https://github.com/hmrc/income-tax-mtd-changelog)

## Support and Reporting Issues

You can create a GitHub issue [here](https://github.com/hmrc/income-tax-mtd-changelog/issues)

## API Reference / Documentation

Available on
the [Individuals Expenses Documentation](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/individuals-expenses-api)

## License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
