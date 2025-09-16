# copilot-instructions.md

## Project Overview
This project is a Java application built with Maven, using Spring Boot for application configuration and dependency injection. Unit and integration tests are written using JUnit 5 (Jupiter).  It uses picocli for parsing command line arguments.  It can perform tasks related to Adobe Experience Manager (AEM).

It can perform the following tasks:
 - Install AEM.
 - Install shim scripts that can start and stop AEM.
 - Watch an AEM log and wait for a string that matches a regular expression to be written to the log.

## Coding Standards
- Use Java 21 or later syntax and features unless otherwise specified.
- Follow standard Java naming conventions.
- Organize code into appropriate packages for an hexagonal architecture (e.g., `domain`, `ports`, `adapters`, `api`, `spi`, `ipi`) (see `architecture.md` for details).
- Use dependency injection for service and repository classes.
- Prefer constructor injection over field injection in Spring components.

## Build and Run
- Build the project: `mvn clean verify`
- Run the application: `mvn spring-boot:run`
- Run tests: `mvn test`

## Testing
- Write tests using JUnit 5 (Jupiter) annotations.
- Place unit tests in `src/test/java` mirroring the main source structure.
- Use Mockito for mocking dependencies in unit tests.
- For integration tests, use `@SpringBootTest` and place them in the same test source tree.

## Best Practices
- Write clear and descriptive method and class-level Javadoc.
- Keep methods short and focused on a single responsibility.
- Handle exceptions appropriately; use custom exceptions where needed.
- Do not use Lombok.
- Ensure all new code is covered by tests.

## Dependencies
- Java 21 or later
- Maven
- Spring Boot (see `pom.xml` for version)
- JUnit 5 (Jupiter)
- Mockito (for unit tests)
- Picocli (for command line parsing)

## Additional Notes
- Update this file if project conventions change.
- For more details, refer to the `README.md` and `pom.xml` files.
