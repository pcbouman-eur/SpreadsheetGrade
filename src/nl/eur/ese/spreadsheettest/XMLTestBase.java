package nl.eur.ese.spreadsheettest;

import nl.eur.spreadsheettest.xml.*;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class XMLTestBase {
    protected String report = "%4d";
    protected Exercise exercise;
    protected Map<String, Integer> testSucceed;
    protected Map<String, Integer> testFailed;
    protected Map<String, Integer> totalTest;
    protected Map<String, Integer> errors;
    protected Map<String, Integer> notImpl;
    protected Map<String, Integer> missing;
    protected boolean traceErrors;
    protected StringBuilder errorTrace;
    private int maxFullGrid = 5;
    private long seed = 54321;

    public XMLTestBase() {
        this.testSucceed = new TreeMap<>();
        this.testFailed = new TreeMap<>();
        this.totalTest = new TreeMap<>();
        this.errors = new TreeMap<>();
        this.notImpl = new TreeMap<>();
        this.missing = new TreeMap<>();
        this.traceErrors = false;
        this.errorTrace = new StringBuilder();
    }

    public static Set<String> getFunctions(Ptg[] tokens) {
        Set<String> functions = new HashSet<>();
        for (Ptg token : tokens) {
            if (token instanceof AttrPtg) {
                AttrPtg ap = (AttrPtg) token;
                if (ap.isSum()) {
                    functions.add("SUM");
                }
                if (ap.isOptimizedIf()) {
                    functions.add("IF");
                }
                if (ap.isOptimizedChoose()) {
                    functions.add("CHOOSE");
                }
            }
            if (token instanceof AbstractFunctionPtg) {
                AbstractFunctionPtg fvp = (AbstractFunctionPtg) token;
                functions.add(fvp.getName().trim().toUpperCase());
            }
        }
        return functions;
    }

    public static String getStackTrace(Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        e.printStackTrace(pw);
        pw.flush();
        return bos.toString();
    }

    public static XMLTest.CompareResult compare(Cell ref, Cell test, double eps, boolean textStrict) {
        if (ref.getCellType() != test.getCellType()) {
            CellType ct = ref.getCellType();
            CellType ct2 = test.getCellType();
            return XMLTest.CompareResult.DIFFERENT_TYPES;
        }
        CellType ct = ref.getCellType();
        if (ct == CellType.FORMULA) {
            if (ref.getCachedFormulaResultType() != test.getCachedFormulaResultType()) {
                return XMLTest.CompareResult.DIFFERENT_TYPES;
            }
            ct = ref.getCachedFormulaResultType();
        }

        if (ct == CellType.STRING) {
            String t1 = ref.getStringCellValue();
            String t2 = test.getStringCellValue();
            if (textStrict) {
                return t1.equals(t2) ? XMLTest.CompareResult.OKAY : XMLTest.CompareResult.DIFFERENT_VALUES;
            } else {
                return t1.trim().toLowerCase().equals(t2.trim().toLowerCase()) ?
                        XMLTest.CompareResult.OKAY : XMLTest.CompareResult.DIFFERENT_VALUES;
            }
        }
        if (ct == CellType.NUMERIC) {
            double d1 = ref.getNumericCellValue();
            double d2 = test.getNumericCellValue();
            if (Math.abs(d1 - d2) <= eps) {
                return XMLTest.CompareResult.OKAY;
            } else {
                return XMLTest.CompareResult.DIFFERENT_VALUES;
            }
        }
        if (ct == CellType.BOOLEAN) {
            if (ref.getBooleanCellValue() == test.getBooleanCellValue()) {
                return XMLTest.CompareResult.OKAY;
            }
            return XMLTest.CompareResult.DIFFERENT_VALUES;
        }
        if (ct == CellType.BLANK) {
            return XMLTest.CompareResult.OKAY;
        }

        return XMLTest.CompareResult.UNSUPPORTED_TYPE;
    }

    public void testAll(XSSFWorkbook ref, XSSFWorkbook test) {
        ModernFunctions.addModernFunctions(ref);
        ModernFunctions.addModernFunctions(test);
        testComparisons(ref, test);
        testStyle(test);
        testRanges(ref, test);
    }

    public void testComparisons(XSSFWorkbook ref, XSSFWorkbook test) {
        if (exercise.getComparisons() == null || exercise.getComparisons().getComparison() == null) {
            return;
        }
        outerLoop:
        for (CompareType comp : exercise.getComparisons().getComparison()) {
            String descr = comp.getDescription().trim().replaceAll("\n", " ");
            String sn = comp.getSheetName();
            Range range = new Range(comp.getRange().trim().replaceAll("\n", " "));
            Sheet refSheet, testSheet;

            if (sn != null) {
                refSheet = ref.getSheet(sn);
                testSheet = test.getSheet(sn);
                if (refSheet == null) {
                    reportError("There is a problem with the assignment: the sheet with name '" + sn
                            + "' could not be found in the reference solution. Please contact us!");
                    continue;
                }
                if (testSheet == null) {
                    reportError("We could not find a sheet with name '" + sn + "' in your solution."
                            + " Make sure you have a sheet with that exact name.");
                    continue;
                }
            } else {
                refSheet = ref.getSheetAt(0);
                testSheet = test.getSheetAt(0);
            }

            for (CellReference cr : range) {
                int row = cr.getRow();
                int col = cr.getCol();
                if (refSheet.getRow(row) == null || refSheet.getRow(row).getCell(col) == null) {
                    reportError("There is a problem with the assignment: cell " + cr.formatAsString()
                            + " in sheet '" + refSheet.getSheetName() + "' could not be found in the "
                            + "reference solution. Please contact us!");
                    continue outerLoop;
                }
                if (testSheet.getRow(row) == null || testSheet.getRow(row).getCell(col) == null) {
                    reportError("We could not find cell " + cr.formatAsString() + " within sheet '" + sn
                            + "' in your solution. Make sure you follow the assignment and do "
                            + " something with this cell.");
                    continue outerLoop;
                }

                Cell refCell = refSheet.getRow(row).getCell(col);
                Cell testCell = testSheet.getRow(row).getCell(col);
                XMLTest.CompareResult res = XMLTestBase.compare(refCell, testCell, comp.getEps(), comp.isTextStrict());
                if (res == XMLTest.CompareResult.UNSUPPORTED_TYPE) {
                    reportError("We could not compare the value in cell " + cr.formatAsString() + " within "
                            + "sheet '" + sn + "' to the reference because it holds an unexpected type of "
                            + "data. Please contact us. ");
                    continue outerLoop;
                }
                if (res == XMLTest.CompareResult.DIFFERENT_TYPES) {
                    if (refCell.getCellType() == CellType.FORMULA) {
                        reportTest("The cell " + cr.formatAsString() + " within sheet '" + sn + "' of your solution " +
                                "should be computed using a formula, but it holds static data. Make sure you " +
                                "use a formula where appropiate.", false);
                    } else {
                        reportTest("The cell " + cr.formatAsString() + " within sheet '" + sn + "' of your solution " +
                                "holds the wrong type of data. Check the assignment to see what type of " +
                                "should be stored in the cell. ", false);
                    }
                    continue outerLoop;
                }
                reportTest(descr, res == XMLTest.CompareResult.OKAY);
            }
        }
    }

    public void testStyle(XSSFWorkbook test) {
        if (exercise == null) {
            throw new IllegalStateException("Cannot iterate over tests if no assignment file has been loaded.");
        }

        if (exercise.getStyles() == null) {
            return;
        }

        XSSFEvaluationWorkbook ewb = XSSFEvaluationWorkbook.create(test);

        for (StyleType style : exercise.getStyles().getStyle()) {
            // Grab the relevant sheet
            String sn = style.getSheetName();
            Sheet sheet;
            if (sn != null) {
                sheet = test.getSheet(sn);
                if (sheet == null) {
                    reportError("We could not find a sheet with name '" + sn + "' in your solution."
                            + " Make sure you have a sheet with that exact name.");
                    continue;
                }
            } else {
                sheet = test.getSheetAt(0);
            }
            int sheetIndex = test.getSheetIndex(sheet);
            String sheetString = "";
            if (sn != null) {
                sheetString = " in sheet '" + sn + "'";
            }

            // Setup data structures for checking
            Set<CellReference> absRefs = new HashSet<>();
            Map<Integer, String> colRefs = new HashMap<>();
            Set<Integer> rowRefs = new HashSet<>();


            Set<String> functionsUsed = new HashSet<>();

            for (AbsoluteType abs : style.getAbsolute()) {
                if (abs.getRange() != null) {
                    Range r = new Range(abs.getRange());
                    for (CellReference cr : r) {
                        absRefs.add(cr);
                    }
                }
                if (abs.getCol() != null) {
                    String colName = abs.getCol().trim().toUpperCase();
                    int col = new CellReference(colName + "1").getCol();
                    colRefs.put(col, colName);
                }
                if (abs.getRow() != null) {
                    int row = abs.getRow();
                    rowRefs.add(row);
                }
            }

            for (Row row : sheet) {
                for (Cell c : row) {
                    //if (c.getCellTypeEnum() == CellType.FORMULA)
                    if (c.getCellType() == CellType.FORMULA) {
                        Ptg[] tokens = FormulaParser.parse(c.getCellFormula(), ewb, FormulaType.CELL, sheetIndex);
                        functionsUsed.addAll(XMLTestBase.getFunctions(tokens));
                        checkReferences(tokens, absRefs, colRefs, rowRefs, sheetString);
                    }
                }
            }

            for (FunctionConstraint req : style.getRequired()) {
                String fname = req.getFunction().trim().toUpperCase();
                reportTest("The function '" + fname + "' is used at least once" + sheetString + "", functionsUsed.contains(fname));
            }

            for (FunctionConstraint req : style.getForbidden()) {
                String fname = req.getFunction().trim().toUpperCase();
                reportTest("The function '" + fname + "' is never used" + sheetString + "", !functionsUsed.contains(fname));
            }
        }


    }

    private void checkReferences(Ptg[] tokens, Set<CellReference> absRefs, Map<Integer, String> colRefs, Set<Integer> rowRefs, String sheetString) {
        for (Ptg token : tokens) {
            if (token instanceof RefPtgBase) {
                RefPtgBase rp = (RefPtgBase) token;
                CellReference cr = new CellReference(rp.getRow(), rp.getColumn());
                if (absRefs.contains(cr)) {
                    boolean test = !rp.isColRelative() && !rp.isRowRelative();
                    reportTest("References to cell " + cr.formatAsString() + sheetString + " are absolute", test);
                }
                if (colRefs.containsKey(rp.getColumn())) {
                    String colName = colRefs.get(rp.getColumn());
                    boolean test = !rp.isColRelative();
                    reportTest("References to column " + colName + sheetString + " are absolute", test);
                }
                if (rowRefs.contains(rp.getRow())) {
                    boolean test = !rp.isRowRelative();
                    reportTest("References to row " + rp.getRow() + " are absolute", test);
                }
            }
        }
    }

    public void testRanges(XSSFWorkbook ref, XSSFWorkbook test) {

        if (exercise == null) {
            throw new IllegalStateException("Cannot iterate over tests if no assignment file has been loaded.");
        }

        if (exercise.getTestcases() == null) {
            return;
        }

        Random ran = new Random(seed);
        report = "%" + exercise.getReportDigits() + "d";
        seed = exercise.getSeed();

        outerLoop:
        for (TestcaseType tc : exercise.getTestcases().getTestcase()) {
            Sheet refSheet, testSheet;

            if (tc.getSheetName() != null) {
                String sn = tc.getSheetName();
                refSheet = ref.getSheet(sn);
                testSheet = test.getSheet(sn);
                if (refSheet == null) {
                    reportError("There is a problem with the assignment: the sheet with name '" + sn
                            + "' could not be found in the reference solution. Please contact us!");
                    continue;
                }
                if (testSheet == null) {
                    reportError("We could not find a sheet with name '" + sn + "' in your solution."
                            + " Make sure you have a sheet with that exact name.");
                    continue;
                }
            } else {
                refSheet = ref.getSheetAt(0);
                testSheet = test.getSheetAt(0);
            }


            String descr = tc.getDescription().trim().replaceAll("\n", " "); //.replaceAll("\\w+", " ");

            List<Range> outputs = tc.getOutput().stream().map(ot -> new Range(ot.getRange())).collect(Collectors.toList());
            ExcelTest et = new ExcelTest(refSheet, testSheet, tc.getEps(), outputs, tc.isTextStrict());

            int cellCount = 0;
            List<InputRangeDouble> irds = new ArrayList<>();
            for (InputType i : tc.getInput()) {
                String range = i.getRange();
                double lb = i.getLb();
                double ub = i.getUb();
                double precision = i.getPrecision();
                InputRangeDouble ird = new InputRangeDouble(range, lb, ub, precision);
                irds.add(ird);
                if (!ird.isTight()) {
                    cellCount += ird.getCellCount();
                }
            }

            if (cellCount <= maxFullGrid) {
                String curTest = descr + " (all combinations of extreme values)";

                List<Assignment> l = new ArrayList<>();
                for (InputRangeDouble ird : irds) {
                    l = ird.expandAssignments(l);
                }

                for (Assignment a : l) {
                    try {
                        reportTest(curTest, et.runTest(a));
                    } catch (NotImplementedFunctionException nie) {
                        notImpl.merge("Usage of function '" + nie.getFunctionName() + "'", 1, (i, j) -> i + j);
                    } catch (NotImplementedException nie) {
                        if (nie.getCause() instanceof NotImplementedFunctionException) {
                            NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
                            String msg = "Usage of unknown function '" + nife.getFunctionName() + "'";
                            msg += " caused by " + nie.getMessage();
                            notImpl.merge(msg, 1, (i, j) -> i + j);
                        } else {
                            notImpl.merge(nie.getMessage(), 1, (i, j) -> i + j);
                        }
                    } catch (NoCellException nce) {
                        missing.merge(nce.getMessage(), 1, (i, j) -> i + j);
                    } catch (Exception e) {
                        reportError(e.getClass().getSimpleName() + " during " + curTest);
                        if (traceErrors) {
                            errorTrace.append("While running " + curTest + " the following Exception was raised\n");
                            errorTrace.append(XMLTestBase.getStackTrace(e));
                            errorTrace.append("\n");
                        }
                        continue outerLoop;
                    }
                }

            } else {
                String curTest = descr + " (random combinations of extreme values)";
                for (int k = 0; k < tc.getRandomCombinations(); k++) {
                    Assignment a = null;
                    for (InputRangeDouble ird : irds) {
                        a = ird.expandRandomUBAssignment(ran, a);
                    }

                    try {
                        reportTest(curTest, et.runTest(a));
                    } catch (NotImplementedFunctionException nie) {
                        notImpl.merge("Usage of function '" + nie.getFunctionName() + "'", 1, (i, j) -> i + j);
                    } catch (NotImplementedException nie) {
                        if (nie.getCause() instanceof NotImplementedFunctionException) {
                            NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
                            String msg = "Usage of unknown function '" + nife.getFunctionName() + "'";
                            msg += " caused by " + nie.getMessage();
                            notImpl.merge(msg, 1, (i, j) -> i + j);
                        } else {
                            notImpl.merge(nie.getMessage(), 1, (i, j) -> i + j);
                        }
                    } catch (NoCellException nce) {
                        missing.merge(nce.getMessage(), 1, (i, j) -> i + j);
                    } catch (Exception e) {
                        reportError(e.getClass().getSimpleName() + " during " + curTest);
                        if (traceErrors) {
                            errorTrace.append("While running " + curTest + " the following Exception was raised\n");
                            errorTrace.append(XMLTestBase.getStackTrace(e));
                            errorTrace.append("\n");
                        }
                        continue outerLoop;
                    }

                }
            }

            String curTest = descr + " (random values)";
            for (int k = 0; k < tc.getRandomDraws(); k++) {
                Assignment a = null;
                for (InputRangeDouble ird : irds) {
                    a = ird.expandRandomAssignment(ran, a);
                }
                try {
                    reportTest(curTest, et.runTest(a));
                } catch (NotImplementedFunctionException nie) {
                    notImpl.merge("Usage of function '" + nie.getFunctionName() + "'", 1, (i, j) -> i + j);
                } catch (NotImplementedException nie) {
                    if (nie.getCause() instanceof NotImplementedFunctionException) {
                        NotImplementedFunctionException nife = (NotImplementedFunctionException) nie.getCause();
                        String msg = "Usage of unknown function '" + nife.getFunctionName() + "'";
                        msg += " caused by " + nie.getMessage();
                        notImpl.merge(msg, 1, (i, j) -> i + j);
                    } else {
                        notImpl.merge(nie.getMessage(), 1, (i, j) -> i + j);
                    }
                } catch (NoCellException nce) {
                    missing.merge(nce.getMessage(), 1, (i, j) -> i + j);
                } catch (Exception e) {
                    reportError(e.getClass().getSimpleName() + " during " + curTest);
                    if (traceErrors) {
                        errorTrace.append("While running " + curTest + " the following Exception was raised\n");
                        errorTrace.append(XMLTestBase.getStackTrace(e));
                        errorTrace.append("\n");
                    }
                    continue outerLoop;
                }
            }
        }
    }

    protected void readExercise(File f) throws JAXBException {

        JAXBContext jc = JAXBContext.newInstance(Exercise.class);
        Unmarshaller u = jc.createUnmarshaller();
        exercise = (Exercise) u.unmarshal(f);
    }

    private void reportTest(String test, boolean b) {
        totalTest.merge(test, 1, (i, j) -> i + j);
        if (b) {
            testSucceed.merge(test, 1, (i, j) -> i + j);
        } else {
            testFailed.merge(test, 1, (i, j) -> i + j);
        }
    }

    private void reportError(String error) {
        errors.merge(error, 1, (i, j) -> i + j);
    }
}
