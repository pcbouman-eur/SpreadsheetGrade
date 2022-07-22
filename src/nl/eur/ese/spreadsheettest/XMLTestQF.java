package nl.eur.ese.spreadsheettest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import picocli.CommandLine;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;

@CommandLine.Command(name="checkqf", mixinStandardHelpOptions=true, description="Spreadsheet checker with Quarterfall output")
public class XMLTestQF extends XMLTestBase implements Callable<Integer> {
	private static final String succeedEmoji = new String(Character.toChars(0x2714));
	private static final String failEmoji = new String(Character.toChars(0x274C));
	private static final String errorEmoji = new String(Character.toChars(0x1F525));
	private static final String notImplEmoji = new String(Character.toChars(0x1F635));
	private static final String missingEmoji = new String(Character.toChars(0x26A0));

	private static final String notImplMsg = "There was a problem during grading, because not "+
			"all Excel functions are implemented in the feedback system";
	private static final String missingMsg = "There was a problem while generating feedback," +
			"because some cells were empty.";

	@CommandLine.Option(names={"-s", "--spec"}, description = "XML file with the specification of the exercise",
						required=true)
	private File exerciseSpec;

	@CommandLine.Option(names={"-r", "--ref"}, description = "Reference .xlsx file with the correct solution",
			required=true)
	private File reference;

	@CommandLine.Parameters(index = "0", description = "The file to check and grade")
	private File checkFile;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output .json file to write feedback to")
	private File output;

	@CommandLine.Option(names={"-c", "--clean"}, description = "Writes a new file even if the .json output already exists")
	private boolean clean;

	private XMLTestQF()
	{
		super();
	}

	public static void main(String [] args)
	{
		int exitCode = new CommandLine(new XMLTestQF()).execute(args);
		System.exit(exitCode);
	}

	public Integer call() throws IOException, JAXBException {
		DatabaseFunctions.register();
		ModernFunctions.register();


		try (XSSFWorkbook ref = new XSSFWorkbook(new FileInputStream(reference));
			 XSSFWorkbook handin = new XSSFWorkbook(new FileInputStream(checkFile)))
		{
			readExercise(exerciseSpec);
			testAll(ref, handin);
		}

		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> result = new TreeMap<>();
		List<String> report = makeQFReport();
		result.put("feedback", report);
		if (output != null) {
			if (output.exists() && !clean) {
				JsonNode node = mapper.readTree(output);
				if (node.isObject() && node instanceof ObjectNode) {
					ObjectNode obj = (ObjectNode) node;
					ArrayNode lst = mapper.createArrayNode();
					report.forEach(lst::add);
					obj.set("feedback", lst);
					mapper.writeValue(output, obj);
				}
				else {
					System.err.println("Structure of output json file is incorrect, it should be an object");
					return 13;
				}
			}
			else {
				mapper.writeValue(output, result);
			}
		}
		else {
			System.out.println(mapper.writeValueAsString(result));
		}
		return 0;
	}

	private String buildFeedback(String header, Map<String,Integer> data, String emoji, boolean includeTotal) {
		return buildFeedback(header, data, null, emoji, includeTotal);
	}

	private String buildFeedback(String header, Map<String,Integer> data, String descr,
								 String emoji, boolean includeTotal) {
		StringBuilder str = new StringBuilder();
		str.append("# "+header+"\n\n");
		if (descr != null && !descr.isEmpty()) {
			str.append(descr);
			str.append("\n\n");
		}
		for (Entry<String, Integer> entry : data.entrySet()) {
			String test = entry.getKey();
			String num = String.format(report, entry.getValue());
			String total = String.format(report, totalTest.get(test));
			str.append("* " + emoji + " [ " + num);
			if (includeTotal) {
				str.append(" out of " + total);

			}
			str.append(" ] : " + test + "\n");
		}
		return str.toString();

	}

	public List<String> makeQFReport() {
		List<String> result = new ArrayList<>();

		if (!testSucceed.isEmpty()) {
			result.add(buildFeedback("Passed Tests", testSucceed, succeedEmoji, true));
		}

		if (!testFailed.isEmpty()) {
			result.add(buildFeedback("Failed Tests", testFailed, failEmoji, true));
		}

		if (!errors.isEmpty()) {
			result.add(buildFeedback("Errors", errors, errorEmoji, false));
		}

		if (!notImpl.isEmpty()) {
			result.add(buildFeedback("Unsupported by Autograder", notImpl, notImplMsg, notImplEmoji, false));
		}

		if (!missing.isEmpty()) {
			result.add(buildFeedback("Missing data", missing, missingMsg, missingEmoji, false));
		}

		return result;
	}

}
