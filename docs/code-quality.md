# Code quality

Style and documentation are enforced with [Checkstyle](https://checkstyle.org) as part of `mvn verify`.
Formatting itself stays with the company-wide IntelliJ default formatter; Checkstyle only checks the rules a
formatter cannot express.

## Running

```bash
mvn checkstyle:check                 # report violations
mvn verify                           # fails on any violation (bound here)
mvn verify -Dcheckstyle.skip=true    # bypass locally
```

## What is enforced

| Family      | Rules                                                                                                    |
|-------------|----------------------------------------------------------------------------------------------------------|
| Imports     | no star imports, no unused imports (intentionally no import-order rule)                                  |
| Line length | 120 chars; package/import lines and URLs in Javadoc are ignored                                          |
| Naming      | type, method, constant, field, local, parameter and package naming                                       |
| Whitespace  | no tabs, no trailing whitespace, newline at EOF, minimal operator spacing                                |
| Braces      | braces always required, same-line opening brace, no empty blocks/fall-through                            |
| Coding      | `equals`/`hashCode` pairing, boolean simplifications, string `==`                                        |
| Javadoc     | public types + public methods require a summary and `@param`/`@return`; accessors and `@Override` exempt |

The ruleset is defined in [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml).

## Current status

The ruleset is stricter than the current code. `mvn verify` stays red until the existing violations
(notably missing Javadoc, star imports, and lines over 120 chars) are fixed and
`DefaultProcessorCatalog`'s long description strings are removed. Fixes are tracked separately.
