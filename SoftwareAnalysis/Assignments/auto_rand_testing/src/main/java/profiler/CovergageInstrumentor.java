package profiler;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.util.*;

import java.io.*;

import java.util.*;


public class CovergageInstrumentor extends BodyTransformer {
    static SootClass markerClass;
    static SootMethod reportFunc, markFunc, reportCodeCoverageFunc;
    static HashMap<String, Integer> nameIndexMap;

    static {
        nameIndexMap = new HashMap<>();
        markerClass = Scene.v().loadClassAndSupport("profiler.StatementMarker");
        reportFunc = markerClass.getMethod("void report()");
        reportCodeCoverageFunc = markerClass.getMethod("void reportCodeCoverage()");
        markFunc = markerClass.getMethod("void mark(java.lang.String,int)");
        Scene.v().setSootClassPath(null);
    }

    protected void insertReportIfNotInit(SootMethod method, Chain units) {
        System.out.println(method.toString());
        String signature = method.getSubSignature();
        boolean isInit = signature.equals("void <init>()");
        if (!isInit) {
            for (Iterator stmtIt = units.snapshotIterator(); stmtIt.hasNext(); ) {
                Stmt stmt = (Stmt) stmtIt.next();
                if ((stmt instanceof ReturnStmt) || (stmt instanceof ReturnVoidStmt)) {
                    InvokeExpr reportExpr = Jimple.v().newStaticInvokeExpr(reportCodeCoverageFunc.makeRef());
                    Stmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
                    units.insertBefore(reportStmt, stmt);
                }
            }
        }
    }

    protected void internalTransform(Body body, String phase, Map options) {
        SootMethod method = body.getMethod();
        Chain units = body.getUnits();
        String className = body.getMethod().getDeclaringClass().toString();
        System.out.println("Instrumenting Method Body: " + body.getMethod().getSignature());
        System.out.println(body.toString());


        if (className.equals("sample.TestInvoke") || className.equals("util.IntArrayUtil")) {
            for (Iterator stmtIt = units.snapshotIterator(); stmtIt.hasNext(); ) {
                Stmt stmt = (Stmt) stmtIt.next();
                if (stmt instanceof JIfStmt) {
                    System.out.println(((JIfStmt) stmt).getTarget().toString());
                }
                if (!(stmt instanceof JIdentityStmt)) {
                    if (!nameIndexMap.containsKey(className)) {
                        nameIndexMap.put(className, -1);
                    }
                    nameIndexMap.put(className, nameIndexMap.get(className) + 1);
                    InvokeExpr markExpr = Jimple.v().newStaticInvokeExpr(markFunc.makeRef(),
                            StringConstant.v(className), IntConstant.v(nameIndexMap.get(className)));
                    Stmt markStmt = Jimple.v().newInvokeStmt(markExpr);
                    units.insertBefore(markStmt, stmt);
                }
            }
            try {
                File file = new File(className + "_" + "statements_num.txt");
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(Integer.toString(nameIndexMap.get(className)));
                writer.close();
            } catch (Exception e) {

            }

        } else {
            insertReportIfNotInit(method, units);
        }


    }
}
