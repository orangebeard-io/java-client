package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.RunType;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.Tool;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;
import io.orangebeard.client.entity.alerting.security.StartSecurityAlertRun;
import io.orangebeard.client.v3.OrangebeardAsyncV3Client;

import org.apache.commons.cli.*;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

public class Cli {

    public static final String CLI_NAME = "Orangebeard CommandLine Utility";

    public static void main(String[] args) {
        OrangebeardProperties config = new OrangebeardProperties();
        OrangebeardAsyncV3Client client = new OrangebeardAsyncV3Client(config);
        CommandLine cmd = parseCommandLine(args);

        if (cmd == null || !cmd.hasOption("x")) {
            printUsageAndExit();
        }

        updateConfigFromCmd(cmd, config);

        String command = cmd.getOptionValue("x");
        RunType runType = determineRunType(cmd);
        Tool alertTool = cmd.hasOption("at") ? Tool.valueOf(cmd.getOptionValue("at").toUpperCase()) : null;

        switch (command) {
            case "start":
                handleStartCommand(client, config, runType, alertTool);
                break;
            case "finish":
                handleFinishCommand(client, cmd, runType);
                break;
            default:
                printUsageAndExit();
        }
    }

    private static CommandLine parseCommandLine(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = commandLineOptions();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            printUsageAndExit();
            return null; // Unreachable, but keeps compiler happy
        }
    }

    private static void printUsageAndExit() {
        new HelpFormatter().printHelp(CLI_NAME, commandLineOptions());
        System.exit(1);
    }

    private static RunType determineRunType(CommandLine cmd) {
        return cmd.hasOption("k") && "security".equals(cmd.getOptionValue("k"))
                ? RunType.SECURITY_ALERT
                : RunType.TEST;
    }

    private static void handleStartCommand(OrangebeardAsyncV3Client client, OrangebeardProperties config, RunType runType, Tool alertTool) {
        if (runType == RunType.TEST) {
            UUID testRunUUID = client.startTestRunAndAwaitRealUUID(new StartV3TestRun(
                    config.getTestSetName(),
                    config.getDescription(),
                    config.getAttributes()
            ));
            System.out.println(testRunUUID);
        } else {
            if (alertTool == null) {
                System.err.println("Error: A tool arg is required when starting an alert run!");
                System.exit(1);
            }
            UUID alertRunUUID = client.startAlertRunAndAwaitRealUUID(new StartSecurityAlertRun(
                    config.getTestSetName(),
                    config.getDescription(),
                    alertTool,
                    ZonedDateTime.now(),
                    config.getAttributes()
            ));
            System.out.println(alertRunUUID);
        }
        System.exit(0);
    }

    private static void handleFinishCommand(OrangebeardAsyncV3Client client, CommandLine cmd, RunType runType) {
        if (!cmd.hasOption("id")) {
            System.out.println("Error: TestRunUuid is required for finish command.");
            printUsageAndExit();
        }

        UUID testRunUuid = parseUUID(cmd.getOptionValue("id"));
        if (testRunUuid == null) {
            System.out.println("Error: Provided test run UUID is not valid.");
            System.exit(1);
        }

        if (runType == RunType.TEST) {
            client.finishTestRunWithRealUUID(testRunUuid, new FinishV3TestRun());
        } else {
            AlertRunStatus status = cmd.hasOption("as") && "INTERRUPTED".equalsIgnoreCase(cmd.getOptionValue("as"))
                    ? AlertRunStatus.INTERRUPTED
                    : AlertRunStatus.COMPLETED;
            client.finishAlertRunWithRealUUID(new FinishSecurityAlertRun(testRunUuid, status, ZonedDateTime.now()));
        }

        System.out.println("Orangebeard report Finished!");
        System.exit(0);
    }

    private static UUID parseUUID(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void updateConfigFromCmd(CommandLine cmd, OrangebeardProperties config) {
        if (cmd.hasOption("e")) {
            config.setEndpoint(cmd.getOptionValue("e"));
        }
        if (cmd.hasOption("t")) {
            config.setAccessToken(UUID.fromString(cmd.getOptionValue("t")));
        }
        if (cmd.hasOption("p")) {
            config.setProjectName(cmd.getOptionValue("p"));
        }
        if (cmd.hasOption("s")) {
            config.setTestSetName(cmd.getOptionValue("s"));
        }
        if (cmd.hasOption("d")) {
            config.setDescription(cmd.getOptionValue("d"));
        }
        if (cmd.hasOption("a")) {
            String attributesString = cmd.getOptionValue("a");
            Set<Attribute> attributes = OrangebeardProperties.extractAttributes(attributesString);
            config.getAttributes().addAll(attributes);
        }
    }

    private static Options commandLineOptions() {
        Options options = new Options();

        options.addOption("e", "endpoint", true, "Your Orangebeard endpoint");
        options.addOption("t", "accessToken", true, "Your Orangebeard Access Token");
        options.addOption("p", "project", true, "Orangebeard Project Name");
        options.addOption("x", "cmd", true, "Command to execute (start/finish)");
        options.addOption("k", "kind", true, "The run kind. \"test\" or \"security\". Defaults to \"test\"");
        options.addOption("s", "testset", true, "The testset name");
        options.addOption("at", "alerttool", true, "The alert tool name (ZAP or BURP)");
        options.addOption("as", "alertrunstatus", true, "Test Alert run status. \"INTERRUPTED\" or \"COMPLETED\". Defaults to \"COMPLETED\"");
        options.addOption("d", "description", true, "The test run description");
        options.addOption("a", "attributes", true, "Test run attributes");
        options.addOption("id", "testRunUuid", true, "The UUID of the test run to finish");
        return options;
    }
}
