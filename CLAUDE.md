# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`io.orangebeard:java-client` is a single-module Maven library (Java 17, version 4.x) used by Orangebeard test-framework listeners to report test runs, suites, tests, steps, logs, attachments and security/code-quality alerts to an Orangebeard server (`/listener/v3/...` REST API). It also ships a small CLI (`io.orangebeard.client.Cli`) to start/finish runs from CI scripts; see README.md for usage.

Runtime dependencies are deliberately minimal: Jackson 3 (`tools.jackson`, annotations still `com.fasterxml.jackson.annotation`), slf4j-api and commons-cli. No Spring (it conflicted with Spring 7 in listener classpaths), no org.json.

## Commands

Run Maven via WSL bash with `mvn` (not `./mvnw`), per the user's global instructions:

```bash
MVN="mvn -s /mnt/c/Users/tomhe/.m2/conf/settings.xml -Ddependency-check.skip=true"
$MVN clean verify                                   # build + all tests + JaCoCo report (target/site/jacoco)
$MVN test -Dtest=OrangebeardAsyncV3ClientTest       # single test class
$MVN test -Dtest=OrangebeardV3ClientTest#methodName # single test method
```

- No component-test subproject here. Tests are JUnit 6 + AssertJ + Mockito + junit-pioneer; HTTP behaviour is tested against `src/test/.../support/FakeOrangebeard`, an in-process JDK `HttpServer` that records requests and can be scripted via `respondWith(...)`.
- From PowerShell/Git Bash use `wsl -e bash -c "..."` (not `bash -lc`, which prints an ssh_askpass error and runs nothing).
- Releases happen in CI on push to `master` (`.github/workflows/master.yml`: removes `-SNAPSHOT`, `mvn -Prelease deploy` to Maven Central). Version bumps are separate "Update version" commits.

## Architecture

Callers are test frameworks reporting live results, so **no public call on `OrangebeardAsyncV3Client` may do I/O on the caller thread**. Layers, bottom to top:

- `OrangebeardV3Client` — synchronous client on the JDK `java.net.http.HttpClient`. Own retry loop (`RetryPolicy`: 4 attempts, 2s doubling; retries IOException, 5xx, 408, 429, never other 4xx), per-request timeout, multipart attachment body built by hand. Throws `OrangebeardClientException` after retries. A failed *start run* call flips a `volatile` flag that turns every later call into a no-op (same when constructed with `connectionWithOrangebeardIsValid=false`, i.e. missing config).
- `v3/V3Client` — the public interface listeners program against.
- `v3/OrangebeardAsyncV3Client` — the implementation listeners use. Returns **temporary UUIDs** immediately; `tasks` maps temp UUID → `CompletableFuture<UUID>` of the real one. Each call is `afterAll(deps, action)`: waits for the futures of everything it depends on (run, suite, test, step, log), then runs the HTTP call on a dedicated 4-thread daemon pool (never `commonPool`, never inline on the caller). A failed dependency skips the dependents; unknown parents fail the future instead of throwing at the caller; failures are logged rate-limited. `finishTestRun`/`finishAlertRun` are the only blocking calls: they wait (bounded by `finishTimeout`, default 5 min) for outstanding requests, then send finish and clear state. Steps/logs/attachments are evicted from `tasks` when their test's finish request completes. When adding an endpoint, follow `afterAll`; `…AndAwaitRealUUID`/`…WithRealUUID` methods are the blocking escape hatches for the CLI.
- `OrangebeardProperties` — config precedence: `orangebeard.properties` → `orangebeard.json` (searched upward from cwd) → system properties → env vars (dots or underscores). Names in `OrangebeardProperty`. A pre-announced run UUID (`testRunUUID`, set by CLI `start`) is pre-registered as a completed task in the async client.
- `Json` — the single shared `JsonMapper`. Dates are written as **epoch seconds with nanos** (`WRITE_DATES_AS_TIMESTAMPS`), the long-standing wire format; Jackson 3 would default to ISO strings. `WireFormatTest` pins this, change it only deliberately.
- `Cli` — `main` just calls `run(args, config, out, err)` which returns the exit code (testable); command-line options override the config *before* the client is created.
- `entity/**` — Lombok request/response DTOs by domain (`test`, `suite`, `step`, `log`, `attachment`, `alerting/{security,codequality}`).

`orangebeard.json` at the repo root is a sample config with placeholder values; tests running from the repo pick it up via the upward search.
