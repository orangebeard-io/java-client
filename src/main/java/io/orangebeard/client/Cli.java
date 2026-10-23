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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class Cli {

    public static final String CLI_NAME = "Orangebeard CommandLine Utility";

    public static void main(String[] args) {
        System.exit(run(args, new OrangebeardProperties(), System.out, System.err));
    }

    /**
     * Runs a CLI command and returns the process exit code. Command line options override the given configuration
     * before the client is created.
     */
    static int run(String[] args, OrangebeardProperties config, PrintStream out, PrintStream err) {
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(commandLineOptions(), args);
        } catch (ParseException e) {
            err.println("Error: " + e.getMessage());
            return usage(out);
        }
        if (!cmd.hasOption("x")) {
            return usage(out);
        }

        try {
            updateConfigFromCmd(cmd, config);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }

        OrangebeardAsyncV3Client client = new OrangebeardAsyncV3Client(config);
        RunType runType = determineRunType(cmd);

        switch (cmd.getOptionValue("x")) {
            case "start":
                return handleStartCommand(client, config, runType, cmd, out, err);
            case "finish":
                return handleFinishCommand(client, cmd, runType, out, err);
            default:
                return usage(out);
        }
    }

    private static int usage(PrintStream out) {
        PrintWriter writer = new PrintWriter(out);
        new HelpFormatter().printHelp(writer, HelpFormatter.DEFAULT_WIDTH, CLI_NAME, null, commandLineOptions(),
                HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
        writer.flush();
        return 1;
    }

    private static RunType determineRunType(CommandLine cmd) {
        return cmd.hasOption("k") && "security".equals(cmd.getOptionValue("k"))
                ? RunType.SECURITY_ALERT
                : RunType.TEST;
    }

    private static int handleStartCommand(OrangebeardAsyncV3Client client, OrangebeardProperties config, RunType runType,
                                          CommandLine cmd, PrintStream out, PrintStream err) {
        UUID runUUID;
        if (runType == RunType.TEST) {
            runUUID = startRun(() -> client.startTestRunAndAwaitRealUUID(new StartV3TestRun(
                    config.getTestSetName(),
                    config.getDescription(),
                    config.getAttributes()
            )), err);
        } else {
            if (!cmd.hasOption("at")) {
                err.println("Error: A tool arg is required when starting an alert run!");
                return 1;
            }
            Tool alertTool;
            try {
                alertTool = Tool.valueOf(cmd.getOptionValue("at").toUpperCase());
            } catch (IllegalArgumentException e) {
                err.println("Error: Unknown alert tool " + cmd.getOptionValue("at") + "! Use ZAP or BURPSUITE.");
                return 1;
            }
            runUUID = startRun(() -> client.startAlertRunAndAwaitRealUUID(new StartSecurityAlertRun(
                    config.getTestSetName(),
                    config.getDescription(),
                    alertTool,
                    ZonedDateTime.now(),
                    config.getAttributes()
            )), err);
        }
        if (runUUID == null) {
            return 1;
        }
        out.println(runUUID);
        return 0;
    }

    private static UUID startRun(Supplier<UUID> start, PrintStream err) {
        try {
            UUID uuid = start.get();
            if (uuid == null) {
                err.println("Error: The run could not be started. Check the Orangebeard configuration.");
            }
            return uuid;
        } catch (RuntimeException e) {
            err.println("Error: The run could not be started: " + e.getMessage());
            return null;
        }
    }

    private static int handleFinishCommand(OrangebeardAsyncV3Client client, CommandLine cmd, RunType runType,
                                           PrintStream out, PrintStream err) {
        if (!cmd.hasOption("id")) {
            err.println("Error: TestRunUuid is required for finish command.");
            return usage(out);
        }

        UUID testRunUuid = parseUUID(cmd.getOptionValue("id"));
        if (testRunUuid == null) {
            err.println("Error: Provided test run UUID is not valid.");
            return 1;
        }

        try {
            if (runType == RunType.TEST) {
                client.finishTestRunWithRealUUID(testRunUuid, new FinishV3TestRun());
            } else {
                AlertRunStatus status = cmd.hasOption("as") && "INTERRUPTED".equalsIgnoreCase(cmd.getOptionValue("as"))
                        ? AlertRunStatus.INTERRUPTED
                        : AlertRunStatus.COMPLETED;
                client.finishAlertRunWithRealUUID(new FinishSecurityAlertRun(testRunUuid, status, ZonedDateTime.now()));
            }
        } catch (RuntimeException e) {
            err.println("Error: The run could not be finished: " + e.getMessage());
            return 1;
        }

        out.println("Orangebeard report Finished!");
        return 0;
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
            config.setAccessToken(UUID.fromString(cmd.getOptionValue("t"))); // IllegalArgumentException when malformed
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
        options.addOption("at", "alerttool", true, "The alert tool name (ZAP or BURPSUITE)");
        options.addOption("as", "alertrunstatus", true, "Test Alert run status. \"INTERRUPTED\" or \"COMPLETED\". Defaults to \"COMPLETED\"");
        options.addOption("d", "description", true, "The test run description");
        options.addOption("a", "attributes", true, "Test run attributes");
        options.addOption("id", "testRunUuid", true, "The UUID of the test run to finish");
        return options;
    }
}
