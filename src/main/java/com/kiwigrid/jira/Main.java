package com.kiwigrid.jira;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import feign.Feign;
import feign.Headers;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	private static final String JIRA_BASE_URI = System.getenv("JIRA_BASE_URI");
	private static final String JIRA_PAT = System.getenv("JIRA_PAT");
	private static final String JIRA_TEMPO_WORKER = System.getenv("JIRA_TEMPO_WORKER");
	private static final Pattern JIRA_ISSUE_PATTERN = Pattern.compile("((?<!([A-Z]{1,10})-?)[A-Z]+-\\d+)");

	private static final DateTimeFormatter HAMSTER_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter JIRA_TEMPO_TIME_FORMAT = DateTimeFormatter.ofPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final TypeReference<List<HamsterActivity>> HAMSTER_XML_TYPE = new TypeReference<>() {
	};

	public static void main(String[] args) throws IOException {
		var xmlMapper = new XmlMapper();
		var hamsterExportFile = new File(args[0]);
		if (!hamsterExportFile.exists() || !hamsterExportFile.canRead()) {
			log.error("Hamster export {} does not exist or is not readable.", hamsterExportFile);
			System.exit(1);
		}
		var hamsterActivities = xmlMapper.readValue(hamsterExportFile, HAMSTER_XML_TYPE);
		log.info("Found {} hamster activities", hamsterActivities.size());
		var jira = createJiraClient();
		long importedCount = 0;
		long importedSeconds = 0;
		for (HamsterActivity activity : hamsterActivities) {
			var optionalWorklog = toWorkLog(activity);
			if (optionalWorklog.isPresent()) {
				var workLog = optionalWorklog.get();
				log.info("Importing {}", workLog);
				jira.postWorkLog(workLog);
				importedCount++;
				importedSeconds += workLog.timeSpentSeconds;
			}
		}
		log.info("Done importing {} records, totalling {} amount of time.", importedCount, Duration.ofSeconds(importedSeconds));
	}

	private static Jira createJiraClient() {
		return Feign.builder()
				.encoder(new JacksonEncoder())
				.decoder(new JacksonDecoder())
				.requestInterceptor(template -> template.header("Authorization", "Bearer %s".formatted(JIRA_PAT)))
				.target(Jira.class, JIRA_BASE_URI);
	}

	private static Optional<WorkLog> toWorkLog(HamsterActivity activity) {
		var tags = activity.tags() != null ? activity.tags().split(",") : new String[0];
		if (tags.length != 1) {
			log.info("Ignoring {}, number of tags is {}", activity, tags.length);
			return Optional.empty();
		}
		if (!JIRA_ISSUE_PATTERN.matcher(tags[0]).matches()) {
			log.info("Ignoring {}, tags does not match issue pattern.", activity);
			return Optional.empty();
		}
		var issueId = tags[0];
		LocalDateTime startDateTime = LocalDateTime.parse(activity.start_time, HAMSTER_DATE_TIME_PATTERN);
		var worklog = new WorkLog(
				Map.of(),
				activity.name,
				issueId,
				startDateTime.format(JIRA_TEMPO_TIME_FORMAT),
				(long) (activity.duration_minutes * 60),
				JIRA_TEMPO_WORKER
		);
		return Optional.of(worklog);
	}

	private interface Jira {
		@RequestLine("POST /rest/tempo-timesheets/4/worklogs/")
		@Headers("Content-Type: application/json")
		void postWorkLog(WorkLog workLog);
	}

	private record WorkLog(
			Map<String, Object> attributes,
			String comment,
			String originTaskId,
			String started,
			long timeSpentSeconds,
			String worker) {
	}

	private record HamsterActivity(
			String name,
			String start_time,
			String end_time,
			double duration_minutes,
			String category,
			String description,
			String tags) {
	}
}