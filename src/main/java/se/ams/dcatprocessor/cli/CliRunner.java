// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.cli;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import se.ams.dcatprocessor.processor.DcatResult;
import se.ams.dcatprocessor.processor.Manager;
import se.ams.dcatprocessor.util.Util;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CliRunner implements ApplicationRunner {

    private final ObjectProvider<Manager> managerProvider;
    private final ApplicationContext context;
    private final ObjectWriter objectWriter;
    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);

    public CliRunner(ObjectProvider<Manager> managerProvider, ApplicationContext context, JsonMapper jsonMapper) {
        this.objectWriter = jsonMapper.writerWithDefaultPrettyPrinter();
        this.managerProvider = managerProvider;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        // No CLI args: run application with webserver
        if (args.getNonOptionArgs().isEmpty()) {
            return;
        }

        // CLI args provided: run command and exit
        ExitCode exitCode = execute(args);
        exitSpringApplication(exitCode);
    }

    /**
     * Runs the CLI command and returns its exit code.
     *
     * @param args the application arguments
     * @return the exit code describing the outcome
     */
    protected ExitCode execute(ApplicationArguments args) {
        List<String> nonOptionArgs = args.getNonOptionArgs();

        // Show help-menu
        if (nonOptionArgs.size() == 1 &&
                CliFlags.HELP_FLAGS.contains(nonOptionArgs.get(0))) {
            printHelp();
            return ExitCode.SUCCESS;
        }

        // Help flag must be used alone
        if (!nonOptionArgs.isEmpty() &&
                CliFlags.HELP_FLAGS.contains(nonOptionArgs.get(0))) {
            System.err.println("Error: help flag must be used alone " + nonOptionArgs);
            return ExitCode.USAGE_ERROR;
        }

        String flag = nonOptionArgs.get(0);

        // Check for unknown flag
        if (!CliFlags.SUPPORTED_FLAGS.contains(flag)) {
            System.err.println("Error: unknown CLI flag " + flag);
            return ExitCode.USAGE_ERROR;
        }

        // Check if a flag is missing its value
        if (nonOptionArgs.size() == 1 &&
                CliFlags.SUPPORTED_FLAGS.contains(nonOptionArgs.get(0))) {
            System.err.println("Error: missing value for flag " + nonOptionArgs.get(0));
            return ExitCode.USAGE_ERROR;
        }

        // Only one flag and value can be used
        if (nonOptionArgs.size() != 2) {
            System.err.println("Error: expected exactly one flag and one value " + nonOptionArgs);
            return ExitCode.USAGE_ERROR;
        }

        String flagValue = nonOptionArgs.get(1);

        // Ensure the flag value is not empty or another flag
        if (flagValue.isBlank() || flagValue.startsWith("-")) {
            System.err.println("Error: missing or invalid value for flag " + flag);
            return ExitCode.USAGE_ERROR;
        }

        return switch (flag) {
            case "-f" -> createDcatFromFile(flagValue);
            case "-d" -> createDcatFromDirectory(flagValue);
            default -> ExitCode.USAGE_ERROR;
        };
    }

    private ExitCode createDcatFromFile(String filename) {
        try {
            Manager manager = managerProvider.getObject();
            DcatResult dcatResult = manager.createDcatFromFile(filename);
            return handleDcatResult(dcatResult);
        } catch (RuntimeException e) {
            logger.error("Unexpected error", e);
            return ExitCode.UNEXPECTED_ERROR;
        }
    }

    private ExitCode createDcatFromDirectory(String dirname) {
        try {
            Manager manager = managerProvider.getObject();
            DcatResult dcatResult = manager.createDcatFromDirectory(dirname);
            return handleDcatResult(dcatResult);
        } catch (RuntimeException e) {
            logger.error("Unexpected error", e);
            return ExitCode.UNEXPECTED_ERROR;
        }
    }

    private ExitCode handleDcatResult(DcatResult dcatResult) {
        if (dcatResult.hasErrors()) {
            System.err.println(dcatResult.errorReport());
            return ExitCode.GENERATION_FAILED;
        } else {
            System.out.println("\n============ Generated RDF ============\n");
            System.out.println(dcatResult.rdf());
            Util.printToFile(dcatResult.rdf(), "dcat.rdf");

            String jsonReport = objectWriter.writeValueAsString(dcatResult.qualityReport());
            System.out.println("\n============ RDF Quality report ============\n");
            System.out.println(jsonReport);
            Util.printToFile(jsonReport, "dcat-quality-report.json");
            return ExitCode.SUCCESS;
        }
    }

    private void exitSpringApplication(ExitCode code) {
        int exitCode = SpringApplication.exit(context, code);
        System.exit(exitCode);
    }

        private void printHelp() {
        System.out.println("""
                DCAT-AP-SE Processor

                Usage:
                java -jar dcat-ap-se-processor.jar -f <file>
                java -jar dcat-ap-se-processor.jar -d <directory>
                java -jar dcat-ap-se-processor.jar -h

                Options:
                  -f <file>       Generate RDF from a single file
                  -d <directory>  Generate RDF from files in a directory
                  -h, -help       Show this help message

                Examples:
                  java -jar dcat-ap-se-processor.jar -f ./file/spec.json
                  java -jar dcat-ap-se-processor.jar -d ./files
                """);
    }
}
