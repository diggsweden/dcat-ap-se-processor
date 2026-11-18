# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-18

### Added

- Add release workflow
- Add exit codes for CLI
- Add renovate config for mise
- Add quality report to CLI, REST and UI
- Add quality check of generated RDF
- Add maven request limit for renovate
- Add renovate.json
- Add repository SECURITY.md
- Add license files and formatter config
- Add coverage for new controller endpoints
- Add vocabulary for dataportal resource-role
- Add reuse and publiccode
- Add CODEOWNERS file
- Add renovate to project
- Add CI for image and test
- Add a CLI interface to convert files and directories
- Add files via upload

### Changed

- Merge pull request #155 from diggsweden/chore/12-prepare-release-1.0.0
- Prepare release 1.0.0
- Merge pull request #147 from diggsweden/fix/23-parser-format-detection
- Merge pull request #150 from diggsweden/fix/149-rdfworker-nested-error-paths
- Merge pull request #133 from diggsweden/renovate/eclipse-temurin-25-jdk
- Update eclipse-temurin:25-jdk docker digest to 83f4a7f
- Merge pull request #141 from diggsweden/fix/140-validate-blank-uri
- Merge pull request #154 from diggsweden/chore/bump-org-json
- Bump org.json to 20260719
- Update dependency org.codehaus.mojo:license-maven-plugin to v2.8.0 (#153)
- Update actions/setup-java action to v6.2.0 (#152)
- Update mise non-major (#151)
- Update mise non-major (#145)
- Update dependency org.commonmark:commonmark to v0.32.0 (#146)
- Merge pull request #144 from diggsweden/ci/143-release-workflow
- Update artifacts.yml for reusable-ci v3
- Merge pull request #138 from diggsweden/chore/137-repo-name-change
- Rename repo to dcat-ap-se-processor
- Update java non-major (#139)
- Merge pull request #131 from diggsweden/feature/126-cli-argumenthantering
- Update CLI help text
- Merge pull request #107 from diggsweden/feature/106-exit-codes
- Update logback config
- Merge pull request #136 from diggsweden/renovate/mise-non-major
- Update mise non-major
- Update dependency org.webjars.npm:monaco-editor to v0.57.0 (#135)
- Update actions/setup-java action to v6.1.0 (#134)
- Merge pull request #130 from diggsweden/chore/mise-update
- Update versions in mise.toml
- Merge pull request #129 from diggsweden/build/fix-orchestrator
- Merge pull request #119 from diggsweden/renovate/diggsweden-reusable-ci-3.x
- Reusable-ci 3.1.0
- Update diggsweden/reusable-ci action to v3
- Update dependency org.commonmark:commonmark to v0.30.1 (#128)
- Merge pull request #112 from diggsweden/feature/10-rdf-quality-report
- Clear quality report when no spec is given
- Update openapi.yaml and .gitignore
- Describe quality check in README
- Read pref from bundle cardinality
- Merge pull request #124 from diggsweden/fix/122-whitespace-in-address
- Merge pull request #121 from diggsweden/docs/120-update-example-files
- Update example files
- Merge pull request #117 from diggsweden/renovate/pin-dependencies
- Pin eclipse-temurin docker tag to 8c0a84e
- Update dependency org.webjars.npm:js-yaml to v4.3.2 (#118)
- Merge pull request #116 from diggsweden/build/114-webjars-locator
- Resolve webjar versions via webjars-locator
- Merge pull request #115 from diggsweden/fix/113-css-encoding-bugg
- Merge pull request #111 from diggsweden/renovate/major-java-major
- Update THIRD-PARTY.txt
- Update dependency org.eclipse.rdf4j:rdf4j-rio-rdfxml to v6
- Merge pull request #110 from diggsweden/renovate/actions-setup-java-6.x
- Update actions/setup-java action to v6
- Merge pull request #109 from diggsweden/renovate/actions-checkout-7.x
- Update actions/checkout action to v7
- Update java non-major (#108)
- Merge pull request #101 from diggsweden/docs/91-update-documentation
- Generate THIRD-PARTY.txt from pom.xml
- Update to match current implementation
- Update to match dcat-ap-se-3.0.1
- Merge pull request #105 from diggsweden/fix/reusable-ci-ref
- Merge pull request #96 from diggsweden/fix/90-thread-safe-error-handling
- Make SpecificationLoader Spring-managed
- Merge pull request #104 from diggsweden/fix/103-mise-warning
- Update github actions (#102)
- Merge pull request #99 from diggsweden/chore/7-activate-renovate
- Merge pull request #98 from diggsweden/renovate/configure
- Update renovate.json
- Merge pull request #97 from diggsweden/chore/7-activate-renovate
- Merge pull request #94 from diggsweden/chore/92-openssf-scorecard
- Update dependencies
- Merge pull request #93 from diggsweden/feature/62-improve-validation-result
- Improve and unify error messages
- Improve error handling flow
- Merge pull request #89 from diggsweden/build/88-fix-release-workflow-dev
- Rename Dockerfile to Containerfile
- Merge pull request #87 from diggsweden/feature/63-update-ui
- Toast messages and permanent light-mode
- New Thymeleaf-based UI
- Merge pull request #86 from diggsweden/feature/84-bundlefile-validation
- Update referns in README.md
- Validation from bundle.json
- Merge pull request #81 from diggsweden/feat/65-api-info
- Swagger-ui and api-info resource
- Merge pull request #80 from diggsweden/fix/70-mapping-of-spatial-and-location
- Merge pull request #79 from diggsweden/fix/71-byteSize-datatype
- Merge pull request #77 from diggsweden/fix/72-adms-status
- Merge pull request #73 from diggsweden/refactor/59-improve-maintainability
- Error handling in DcatPropertyHandler and ApiDefinitionParser
- Clean up converter classes and manager
- Merge pull request #76 from diggsweden/feature/66-health-endpoint
- Prometheus metrics via actuator endpoint
- Merge pull request #78 from diggsweden/chore/69-readme-badges
- Merge pull request #82 from diggsweden/fix/opengrep-issue
- Merge pull request #60 from diggsweden/feat/50-upgrade-dcat-ap-se-version
- Upgrade dcat spec to 3.0.1
- Merge pull request #58 from diggsweden/refactor/55-refactor-tests
- Improve test readability and structure
- Merge pull request #57 from diggsweden/chore/56-add-codeowners
- Merge pull request #54 from diggsweden/chore/48-correcting-pr-workflow-errors
- License to EUPL-1.2, badge for open-ssf
- Reuse config for licenses, MIT licens for tachyons.min.css
- Pin image to specific SHA256
- Markdown lint config and mardown fix
- Merge pull request #51 from diggsweden/feature/46-refaktorisering-part-2
- Apidefinitionparser and test class
- Converter and extending classes
- Manager and errorreporter
- Cli and controller
- Merge pull request #53 from diggsweden/fix/52-open-ssf-scorecard
- Merge pull request #45 from diggsweden/feature/41-uppdatera-github-workflows
- Setup for xmllint in justfile
- Config for reuse, licenses in separate folder
- Configuration for pr-workflow, removed dependency-check-maven
- Update workflows, remove old workflows
- Merge pull request #39 from diggsweden/feature/5-granska-befintlig-dokumenation
- README updated
- Merge pull request #40 from diggsweden/feature/6-refaktorisera-valda-delar
- Renaming two tests
- Dependency injection, CliRunner, cleanup and tests.
- Merge pull request #38 from diggsweden/feature/35-livscykelhantering-beroenden
- Deleting codeql.yml and testing updated default setting for CodeQL workflow
- Updated CodeQL workflow, older "default" mode don't work with java25. This new workflow is using the advanced settings which requires .yml file and is updated with the latest v4 and v6 api
- Updated java version in github workflow
- Changing docker image to full JDK
- Upgraded Java and dependencies. Removed test dependency from Application.java (JUnit Assert) with minor code change (2 unittests for coverage).
- Merge pull request #34 from perweij/patch-1
- Update java version to match pom file in build.yml
- Merge pull request #33 from diggsweden/18-förvaltning-av-gamla-beroenden-1
- Using fully qualified image name now
- Using java 17 in docker file
- Upgraded spring boot to 3.2.0 and upgraded snaketaml to 2.2
- Merge pull request #16 from janderssonse/main
- Merge pull request #15 from jenniferskoglund/main
- Improve README part 2
- Improve README
- Try to build a docker image
- Merge pull request #2 from weipe/add-cli
- Delete maven.yml
- Merge pull request #1 from weipe/build-artifact-in-dockerfile
- Use java file separator property instead of hardcoded Windows separator
- Build artifact in Dockerfile
- Rename dockerfile to Dockerfile, to adhere to docker standard
- Create maven.yml
- Delete maven-publish.yml
- Create maven-publish.yml
- Fixed security and bugs
- Changed conformsTo so that it works in Dataportalen
- Changed completed schematics
- Changed Checksum to work with dataportalen
- Updated tests and testfiles to reflect src
- Changed namesspaces to correct uri
- Fixed problems with error messages and added about in missing tags
- Changed in example file to not be duplicate
- Updated status tag in README
- Initial commit

### Fixed

- Format detection in ApiDefinitionParser
- Include parent path in validation errors
- Report full path for nested block errors
- Validate blank and null URI values
- Fix lint problem in readme
- Validate CLI arguments and add help menu
- Write warn/error logs to stderr
- Resolve hadolint issue DL3066
- Fix orchestrator for workflows
- Strip whitespace around address parts
- Css encoding issue
- Update test for rdf4j 6.x.x
- Update release version for reusable-ci-ref
- Coverage for new error handling
- Correct malformed chainguard/jdk
- Verify validation from bundle.json
- Unit and integration tests for spatial fix
- Use locn:geometry property
- Datatype for spatial properties
- Datatype for byteSize
- Use EU Publication URIs for adms:status
- Update test JSON file and minor adjustments
- Solve misc TODOs
- Ignore workflow files from OpenGrep scanning
- Test for dcat spec v3.0.1
- Openssf-scorcard and mvn test
- Error from trivy report

### Removed

- Remove unused methods
- Remove singleton state from error handling
- Remove mise paranoid setting
- Delete renovate.json
- Removed hibernate-json-org-contributor from mopano
- Removed (with comment) failing tests, will be fixed in the future
- Removed jsoup and added comments in readme about conformsTo


<!-- generated by git-cliff -->
