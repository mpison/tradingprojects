package com.quantlabs.stockApp.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * ExpressionStudioSwing
 * - Variables + Expressions management
 * - Common fixed variables displayed separately (read-only)
 * - Expression builder opens as dialog (Edit...) NOT as a tab
 * - Inline AST variables supported (mode INLINE_EXPRESSION)
 * - Date/Time pickers for variable literals + literal nodes
 * - Function shortcuts: minutesOfDay, betweenTime, isWeekend, inDateRange
 * - Validation graph (deps + missing refs + cycles + topo order)
 * - Save/Load JSON via JsonObjectMapper (Jackson)
 *
 * Java 8+
 */
public class ExpressionStudioSwing extends JFrame {

    // =========================
    // Enums / types
    // =========================
    public enum ValueType { NUMBER, BOOLEAN, DATE, TIME, DATETIME, STRING }
    public enum VarValueMode { LITERAL, EXPRESSION_REF, INLINE_EXPRESSION }
    public enum JoinOp { AND, OR }
    public enum CmpOp {
        GT(">"), GE(">="), LT("<"), LE("<="), EQ("=="), NE("!=");
        public final String s;
        CmpOp(String s){ this.s=s; }
        @Override public String toString(){ return s; }
    }
    public enum ArithOp {
        ADD("+"), SUB("-"), MUL("*"), DIV("/");
        public final String s;
        ArithOp(String s){ this.s=s; }
        @Override public String toString(){ return s; }
    }
    public enum UnaryOp {
        NEG("-"), NOT("NOT");
        public final String s;
        UnaryOp(String s){ this.s=s; }
        @Override public String toString(){ return s; }
    }

    /**
     * Functions:
     * - Math: abs, sqrt, min, max, pow
     * - Date/Time parts (return NUMBER):
     *    year(x), month(x), day(x), dayOfWeek(x),
     *    hour(x), minute(x), second(x),
     *    epochSeconds(datetime)
     * - Shortcuts:
     *    minutesOfDay(timeOrDateTime) -> NUMBER
     *    betweenTime(timeOrDateTime, "HH:mm", "HH:mm") -> BOOLEAN (overnight supported)
     *    isWeekend(dateOrDateTime) -> BOOLEAN
     *    inDateRange(dateOrDateTime, "yyyy-MM-dd", "yyyy-MM-dd") -> BOOLEAN
     */
    public enum Func {
        ABS("abs",1), SQRT("sqrt",1), MIN("min",2), MAX("max",2), POW("pow",2),

        YEAR("year",1), MONTH("month",1), DAY("day",1), DAY_OF_WEEK("dayOfWeek",1),
        HOUR("hour",1), MINUTE("minute",1), SECOND("second",1),
        EPOCH_SECONDS("epochSeconds",1),

        MINUTES_OF_DAY("minutesOfDay",1),
        BETWEEN_TIME("betweenTime",3),
        IS_WEEKEND("isWeekend",1),
        IN_DATE_RANGE("inDateRange",3);

        public final String name;
        public final int arity;
        Func(String n, int a){ name=n; arity=a; }
        @Override public String toString(){ return name; }
    }

    // =========================
    // Registry + Variable model
    // =========================
    public static class VarDef {
        public String name;
        public ValueType type = ValueType.NUMBER;
        public VarValueMode mode = VarValueMode.LITERAL;

        // LITERAL:
        public String literalValue = "";

        // EXPRESSION_REF:
        public String expressionName = "";

        // INLINE_EXPRESSION:
        public Expr inlineExpr = null;

        // NEW:
        public boolean readOnly = false;     // cannot modify/rename/delete
        public boolean commonFixed = false;  // shown in separate UI section

        public VarDef() {}

        public VarDef(String name, ValueType type, VarValueMode mode,
                      String literalValue, String exprName, Expr inlineExpr,
                      boolean readOnly, boolean commonFixed) {
            this.name = name;
            this.type = type;
            this.mode = mode;
            this.literalValue = literalValue;
            this.expressionName = exprName;
            this.inlineExpr = inlineExpr;
            this.readOnly = readOnly;
            this.commonFixed = commonFixed;
        }
    }

    public static class Registry {
        public Map<String, VarDef> vars = new LinkedHashMap<>();
        public Map<String, Expr> expressions = new LinkedHashMap<>();
        public String currentExpressionName = "";

        public List<String> variableNames() { return new ArrayList<>(vars.keySet()); }
        public List<String> expressionNames() { return new ArrayList<>(expressions.keySet()); }
    }

    // =========================
    // AST / Expr Polymorphism (Jackson)
    // =========================
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VarRef.class, name = "VAR"),
            @JsonSubTypes.Type(value = ExprRef.class, name = "REF"),
            @JsonSubTypes.Type(value = Literal.class, name = "LIT"),
            @JsonSubTypes.Type(value = Unary.class, name = "UNARY"),
            @JsonSubTypes.Type(value = BinaryArith.class, name = "ARITH"),
            @JsonSubTypes.Type(value = FuncCall.class, name = "FUNC"),
            @JsonSubTypes.Type(value = Compare.class, name = "CMP"),
            @JsonSubTypes.Type(value = BoolGroup.class, name = "BOOL")
    })
    public interface Expr {
        ValueType type(Registry reg);
        String toExprString();

        @JsonIgnore Object eval(EvalContext ctx, StringBuilder trace);

        @JsonIgnore void collectVarRefs(Set<String> out);
        @JsonIgnore void collectExprRefs(Set<String> out);

        @JsonIgnore String displayName();
    }

    // =========================
    // Eval Context
    // =========================
    public static class EvalContext {
        public final Registry reg;

        public EvalContext(Registry reg) { this.reg = reg; }

        public Object resolveExpression(String exprName, StringBuilder trace, LinkedHashSet<String> stack) {
            Expr e = reg.expressions.get(exprName);
            if (e == null) throw new IllegalArgumentException("Unknown expression: " + exprName);

            if (!stack.add(exprName)) {
                throw new IllegalArgumentException("Cyclic expression reference detected: " + stack);
            }
            trace.append("REF @").append(exprName).append(" {\n");
            Object out = e.eval(this, trace);
            trace.append("} REF @").append(exprName).append(" => ").append(out).append("\n");
            stack.remove(exprName);
            return out;
        }

        public Object resolveVariable(String varName, StringBuilder trace) {
            VarDef v = reg.vars.get(varName);
            if (v == null) throw new IllegalArgumentException("Unknown variable: " + varName);

            Object raw;
            if (v.mode == VarValueMode.LITERAL) {
                raw = parseByType(v.type, v.literalValue, "Variable '" + varName + "'");
            } else if (v.mode == VarValueMode.EXPRESSION_REF) {
                String exprName = safe(v.expressionName);
                if (exprName.isEmpty()) throw new IllegalArgumentException("Variable '" + varName + "' expressionName is empty");
                raw = resolveExpression(exprName, trace, new LinkedHashSet<String>());
            } else { // INLINE_EXPRESSION
                if (v.inlineExpr == null) throw new IllegalArgumentException("Variable '" + varName + "' inlineExpr is empty");
                trace.append("VAR ").append(varName).append(" INLINE_EXPR {\n");
                raw = v.inlineExpr.eval(this, trace);
                trace.append("} VAR ").append(varName).append(" INLINE_EXPR => ").append(raw).append("\n");
            }

            Object coerced = coerceToType(v.type, raw, "Variable '" + varName + "'");
            trace.append("VAR ").append(varName)
                    .append(" (").append(v.type).append(", ").append(v.mode).append(") = ")
                    .append(coerced).append("\n");
            return coerced;
        }
    }

    // =========================
    // Expr Nodes
    // =========================
    public static class VarRef implements Expr {
        public String name = "";

        public VarRef() {}
        public VarRef(String name){ this.name = name; }

        public ValueType type(Registry reg) {
            VarDef v = reg.vars.get(name);
            return v == null ? ValueType.NUMBER : v.type;
        }
        public String toExprString() { return safe(name); }
        public Object eval(EvalContext ctx, StringBuilder trace) { return ctx.resolveVariable(safe(name), trace); }
        public void collectVarRefs(Set<String> out){ out.add(safe(name)); }
        public void collectExprRefs(Set<String> out){}
        public String displayName(){ return "VAR: " + safe(name); }
    }

    public static class ExprRef implements Expr {
        public String exprName = "";

        public ExprRef() {}
        public ExprRef(String exprName){ this.exprName = exprName; }

        public ValueType type(Registry reg) {
            Expr e = reg.expressions.get(exprName);
            return e == null ? ValueType.NUMBER : e.type(reg);
        }
        public String toExprString(){ return "@" + safe(exprName); }
        public Object eval(EvalContext ctx, StringBuilder trace){
            return ctx.resolveExpression(safe(exprName), trace, new LinkedHashSet<String>());
        }
        public void collectVarRefs(Set<String> out){}
        public void collectExprRefs(Set<String> out){ out.add(safe(exprName)); }
        public String displayName(){ return "REF: @" + safe(exprName); }
    }

    public static class Literal implements Expr {
        public ValueType litType = ValueType.NUMBER;
        public String value = "0";

        public Literal() {}
        public Literal(ValueType t, String v){ litType=t; value=v; }

        public ValueType type(Registry reg){ return litType; }

        public String toExprString() {
            String v = safe(value);
            if (litType == ValueType.STRING) return "\"" + v.replace("\"", "\\\"") + "\"";
            if (litType == ValueType.DATE || litType == ValueType.TIME || litType == ValueType.DATETIME) return "#" + v;
            return v;
        }

        public Object eval(EvalContext ctx, StringBuilder trace) {
            Object parsed = parseByType(litType, value, "Literal");
            trace.append("LIT ").append(litType).append(" ").append(value == null ? "" : value.trim()).append("\n");
            return parsed;
        }

        public void collectVarRefs(Set<String> out){}
        public void collectExprRefs(Set<String> out){}
        public String displayName(){ return "LIT: " + litType + " " + safe(value); }
    }

    public static class Unary implements Expr {
        public UnaryOp op = UnaryOp.NEG;
        public Expr child = new Literal(ValueType.NUMBER, "0");

        public Unary() {}
        public Unary(UnaryOp op, Expr child){ this.op=op; this.child=child; }

        public ValueType type(Registry reg){ return op == UnaryOp.NOT ? ValueType.BOOLEAN : ValueType.NUMBER; }

        public String toExprString(){
            if (op == UnaryOp.NOT) return "(NOT " + child.toExprString() + ")";
            return "(-" + child.toExprString() + ")";
        }

        public Object eval(EvalContext ctx, StringBuilder trace) {
            Object c = child.eval(ctx, trace);
            if (op == UnaryOp.NOT) {
                boolean r = asBool(c);
                r = !r;
                trace.append("NOT => ").append(r).append("\n");
                return r;
            } else {
                double r = -asNum(c);
                trace.append("NEG => ").append(r).append("\n");
                return r;
            }
        }

        public void collectVarRefs(Set<String> out){ child.collectVarRefs(out); }
        public void collectExprRefs(Set<String> out){ child.collectExprRefs(out); }
        public String displayName(){ return "UNARY " + op; }
    }

    public static class BinaryArith implements Expr {
        public ArithOp op = ArithOp.ADD;
        public Expr left = new Literal(ValueType.NUMBER, "0");
        public Expr right = new Literal(ValueType.NUMBER, "0");

        public BinaryArith() {}
        public BinaryArith(ArithOp op, Expr l, Expr r){ this.op=op; left=l; right=r; }

        public ValueType type(Registry reg){ return ValueType.NUMBER; }

        public String toExprString(){ return "(" + left.toExprString() + " " + op.s + " " + right.toExprString() + ")"; }

        public Object eval(EvalContext ctx, StringBuilder trace) {
            double a = asNum(left.eval(ctx, trace));
            double b = asNum(right.eval(ctx, trace));

            double r;
            if (op == ArithOp.ADD) r = a + b;
            else if (op == ArithOp.SUB) r = a - b;
            else if (op == ArithOp.MUL) r = a * b;
            else r = a / b;

            trace.append("ARITH ").append(a).append(" ").append(op.s).append(" ").append(b).append(" => ").append(r).append("\n");
            return r;
        }

        public void collectVarRefs(Set<String> out){ left.collectVarRefs(out); right.collectVarRefs(out); }
        public void collectExprRefs(Set<String> out){ left.collectExprRefs(out); right.collectExprRefs(out); }
        public String displayName(){ return "ARITH " + op; }
    }

    public static class FuncCall implements Expr {
        public Func func = Func.ABS;
        public List<Expr> args = new ArrayList<>();

        public FuncCall(){ setFunc(Func.ABS); }
        public FuncCall(Func f){ setFunc(f); }

        public void setFunc(Func f){
            func = f;
            if (args == null) args = new ArrayList<>();
            while (args.size() < func.arity) args.add(new Literal(ValueType.NUMBER, "0"));
            while (args.size() > func.arity) args.remove(args.size()-1);
        }

        public ValueType type(Registry reg){
            if (func == Func.BETWEEN_TIME || func == Func.IS_WEEKEND || func == Func.IN_DATE_RANGE) return ValueType.BOOLEAN;
            return ValueType.NUMBER;
        }

        public String toExprString() {
            List<String> parts = new ArrayList<>();
            for (Expr e : args) parts.add(e.toExprString());
            return func.name + "(" + join(parts, ", ") + ")";
        }

        public Object eval(EvalContext ctx, StringBuilder trace) {
            // math group
            if (func == Func.ABS || func == Func.SQRT || func == Func.MIN || func == Func.MAX || func == Func.POW) {
                double[] v = new double[args.size()];
                for (int i=0;i<args.size();i++) v[i] = asNum(args.get(i).eval(ctx, trace));
                double r;
                if (func == Func.ABS) r = Math.abs(v[0]);
                else if (func == Func.SQRT) r = Math.sqrt(v[0]);
                else if (func == Func.MIN) r = Math.min(v[0], v[1]);
                else if (func == Func.MAX) r = Math.max(v[0], v[1]);
                else r = Math.pow(v[0], v[1]);
                trace.append("FUNC ").append(func.name).append(" => ").append(r).append("\n");
                return r;
            }

            // shortcuts
            if (func == Func.MINUTES_OF_DAY) {
                Object x = args.get(0).eval(ctx, trace);
                int m = minutesOfDayOf(x);
                trace.append("FUNC minutesOfDay(").append(x).append(") => ").append(m).append("\n");
                return (double)m;
            }
            if (func == Func.BETWEEN_TIME) {
                Object x = args.get(0).eval(ctx, trace);
                Object a = args.get(1).eval(ctx, trace);
                Object b = args.get(2).eval(ctx, trace);

                LocalTime start = parseTimeLiteral(a);
                LocalTime end = parseTimeLiteral(b);

                int cur = minutesOfDayOf(x);
                int s = start.getHour()*60 + start.getMinute();
                int e = end.getHour()*60 + end.getMinute();

                boolean ok;
                if (s <= e) ok = (cur >= s && cur <= e);
                else ok = (cur >= s || cur <= e); // overnight session
                trace.append("FUNC betweenTime(").append(x).append(",").append(start).append(",").append(end).append(") => ").append(ok).append("\n");
                return ok;
            }
            if (func == Func.IS_WEEKEND) {
                Object x = args.get(0).eval(ctx, trace);
                DayOfWeek dow;
                if (x instanceof LocalDate) dow = ((LocalDate) x).getDayOfWeek();
                else if (x instanceof LocalDateTime) dow = ((LocalDateTime) x).getDayOfWeek();
                else throw new IllegalArgumentException("isWeekend(x) expects DATE or DATETIME, got " + typeName(x));

                boolean ok = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
                trace.append("FUNC isWeekend(").append(x).append(") => ").append(ok).append("\n");
                return ok;
            }
            if (func == Func.IN_DATE_RANGE) {
                Object x = args.get(0).eval(ctx, trace);
                Object a = args.get(1).eval(ctx, trace);
                Object b = args.get(2).eval(ctx, trace);

                LocalDate d;
                if (x instanceof LocalDate) d = (LocalDate) x;
                else if (x instanceof LocalDateTime) d = ((LocalDateTime) x).toLocalDate();
                else throw new IllegalArgumentException("inDateRange(x) expects DATE or DATETIME, got " + typeName(x));

                LocalDate start = parseDateLiteral(a);
                LocalDate end = parseDateLiteral(b);

                boolean ok = (!d.isBefore(start)) && (!d.isAfter(end));
                trace.append("FUNC inDateRange(").append(d).append(",").append(start).append(",").append(end).append(") => ").append(ok).append("\n");
                return ok;
            }

            // date/time parts + epochSeconds
            Object x = args.get(0).eval(ctx, trace);
            double out;
            if (func == Func.YEAR) out = yearOf(x);
            else if (func == Func.MONTH) out = monthOf(x);
            else if (func == Func.DAY) out = dayOf(x);
            else if (func == Func.DAY_OF_WEEK) out = dayOfWeekOf(x);
            else if (func == Func.HOUR) out = hourOf(x);
            else if (func == Func.MINUTE) out = minuteOf(x);
            else if (func == Func.SECOND) out = secondOf(x);
            else out = epochSecondsOf(x);

            trace.append("FUNC ").append(func.name).append("(").append(x).append(") => ").append(out).append("\n");
            return out;
        }

        public void collectVarRefs(Set<String> out){ for (Expr e : args) e.collectVarRefs(out); }
        public void collectExprRefs(Set<String> out){ for (Expr e : args) e.collectExprRefs(out); }
        public String displayName(){ return "FUNC " + func.name; }
    }

    public static class Compare implements Expr {
        public CmpOp op = CmpOp.GT;
        public Expr left = new VarRef("");
        public Expr right = new Literal(ValueType.NUMBER, "0");

        public Compare() {}
        public Compare(CmpOp op, Expr l, Expr r){ this.op=op; left=l; right=r; }

        public ValueType type(Registry reg){ return ValueType.BOOLEAN; }

        public String toExprString(){ return "(" + left.toExprString() + " " + op.s + " " + right.toExprString() + ")"; }

        public Object eval(EvalContext ctx, StringBuilder trace){
            Object a = left.eval(ctx, trace);
            Object b = right.eval(ctx, trace);
            boolean r = compareObjects(a, b, op);
            trace.append("CMP ").append(a).append(" ").append(op.s).append(" ").append(b).append(" => ").append(r).append("\n");
            return r;
        }

        public void collectVarRefs(Set<String> out){ left.collectVarRefs(out); right.collectVarRefs(out); }
        public void collectExprRefs(Set<String> out){ left.collectExprRefs(out); right.collectExprRefs(out); }
        public String displayName(){ return "CMP " + op; }
    }

    public static class BoolGroup implements Expr {
        public JoinOp op = JoinOp.AND;
        public List<Expr> children = new ArrayList<>();

        public BoolGroup() {}
        public BoolGroup(JoinOp op){ this.op=op; }

        public ValueType type(Registry reg){ return ValueType.BOOLEAN; }

        public String toExprString() {
            if (children == null || children.isEmpty()) return "( )";
            if (children.size() == 1) return children.get(0).toExprString();
            String join = " " + op.name() + " ";
            List<String> parts = new ArrayList<>();
            for (Expr e : children) parts.add(e.toExprString());
            return "(" + join(parts, join) + ")";
        }

        public Object eval(EvalContext ctx, StringBuilder trace) {
            if (children == null || children.isEmpty()) return false;

            trace.append("BOOL ").append(op).append(" {\n");

            boolean agg = (op == JoinOp.AND);
            for (int i=0;i<children.size();i++) {
                boolean v = asBool(children.get(i).eval(ctx, trace));
                if (op == JoinOp.AND) {
                    agg = agg && v;
                    if (!agg) { trace.append("  AND short-circuit => false\n"); break; }
                } else {
                    agg = (i==0) ? v : (agg || v);
                    if (agg) { trace.append("  OR short-circuit => true\n"); break; }
                }
            }

            trace.append("} => ").append(agg).append("\n");
            return agg;
        }

        public void collectVarRefs(Set<String> out){ for (Expr e : children) e.collectVarRefs(out); }
        public void collectExprRefs(Set<String> out){ for (Expr e : children) e.collectExprRefs(out); }
        public String displayName(){ return "BOOL GROUP (" + op + ")"; }
    }

    // =========================
    // Swing Tree Node wrapper
    // =========================
    static class ExprTreeNode extends DefaultMutableTreeNode {
        ExprTreeNode(Expr e){ super(e); }
        Expr expr(){ return (Expr) getUserObject(); }
    }

    // =========================
    // Jackson ObjectMapper helper
    // =========================
    public static final class JsonObjectMapper {
        private static final ObjectMapper MAPPER = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        private JsonObjectMapper(){}

        public static void save(File file, Registry reg) throws IOException {
            MAPPER.writeValue(file, reg);
        }
        public static Registry load(File file) throws IOException {
            return MAPPER.readValue(file, Registry.class);
        }
    }

    // =========================
    // Expr walker for renames
    // =========================
    static final class ExprWalker {
        static void renameVarRefs(Expr e, String oldName, String newName) {
            if (e instanceof VarRef) {
                VarRef v = (VarRef) e;
                if (oldName.equals(v.name)) v.name = newName;
            }
            for (Expr c : childrenOf(e)) renameVarRefs(c, oldName, newName);
        }
        static void renameExprRefs(Expr e, String oldName, String newName) {
            if (e instanceof ExprRef) {
                ExprRef r = (ExprRef) e;
                if (oldName.equals(r.exprName)) r.exprName = newName;
            }
            for (Expr c : childrenOf(e)) renameExprRefs(c, oldName, newName);
        }

        static List<Expr> childrenOf(Expr e) {
            if (e instanceof Unary) {
                return Collections.singletonList(((Unary) e).child);
            }
            if (e instanceof BinaryArith) {
                BinaryArith b = (BinaryArith) e;
                return Arrays.asList(b.left, b.right);
            }
            if (e instanceof Compare) {
                Compare c = (Compare) e;
                return Arrays.asList(c.left, c.right);
            }
            if (e instanceof BoolGroup) {
                BoolGroup g = (BoolGroup) e;
                return g.children == null ? Collections.<Expr>emptyList() : g.children;
            }
            if (e instanceof FuncCall) {
                FuncCall f = (FuncCall) e;
                return f.args == null ? Collections.<Expr>emptyList() : f.args;
            }
            return Collections.emptyList();
        }
    }

    // =========================
    // Validation Graph
    // =========================
    static class GraphReport {
        Map<String, Set<String>> exprToExpr = new LinkedHashMap<>();
        Map<String, Set<String>> exprToVar  = new LinkedHashMap<>();
        Map<String, Set<String>> varToExpr  = new LinkedHashMap<>();

        List<String> missingVars = new ArrayList<>();
        List<String> missingExprs = new ArrayList<>();
        List<List<String>> cycles = new ArrayList<>();
        List<String> topoOrder = new ArrayList<>();
    }

    static GraphReport buildGraph(Registry reg) {
        GraphReport r = new GraphReport();

        for (String e : reg.expressionNames()) {
            r.exprToExpr.put(e, new LinkedHashSet<String>());
            r.exprToVar.put(e, new LinkedHashSet<String>());
        }
        for (String v : reg.variableNames()) {
            r.varToExpr.put(v, new LinkedHashSet<String>());
        }

        // expressions -> (vars, exprs)
        for (Map.Entry<String, Expr> en : reg.expressions.entrySet()) {
            String exprName = en.getKey();
            Expr root = en.getValue();

            Set<String> varRefs = new LinkedHashSet<String>();
            Set<String> exprRefs = new LinkedHashSet<String>();
            root.collectVarRefs(varRefs);
            root.collectExprRefs(exprRefs);

            r.exprToVar.get(exprName).addAll(varRefs);
            r.exprToExpr.get(exprName).addAll(exprRefs);

            for (String v : varRefs) if (!reg.vars.containsKey(v))
                r.missingVars.add("Expr '" + exprName + "' missing var: " + v);
            for (String e2 : exprRefs) if (!reg.expressions.containsKey(e2))
                r.missingExprs.add("Expr '" + exprName + "' missing expr: " + e2);
        }

        // variables -> expressions (ref + inline)
        for (VarDef v : reg.vars.values()) {
            if (!r.varToExpr.containsKey(v.name)) r.varToExpr.put(v.name, new LinkedHashSet<String>());

            if (v.mode == VarValueMode.EXPRESSION_REF) {
                String ref = safe(v.expressionName);
                if (ref.isEmpty() || !reg.expressions.containsKey(ref)) {
                    r.missingExprs.add("Var '" + v.name + "' missing expr ref: " + ref);
                } else {
                    r.varToExpr.get(v.name).add(ref);
                }
            } else if (v.mode == VarValueMode.INLINE_EXPRESSION && v.inlineExpr != null) {
                Set<String> exprRefs = new LinkedHashSet<String>();
                v.inlineExpr.collectExprRefs(exprRefs);
                for (String er : exprRefs) {
                    if (!reg.expressions.containsKey(er)) r.missingExprs.add("Var '" + v.name + "' inline missing expr: " + er);
                    else r.varToExpr.get(v.name).add(er);
                }
                Set<String> varRefs = new LinkedHashSet<String>();
                v.inlineExpr.collectVarRefs(varRefs);
                for (String vr : varRefs) if (!reg.vars.containsKey(vr)) r.missingVars.add("Var '" + v.name + "' inline missing var: " + vr);
            }
        }

        detectCyclesAndTopo(r, reg);
        return r;
    }

    static void detectCyclesAndTopo(GraphReport r, Registry reg) {
        Map<String, Integer> state = new HashMap<String, Integer>(); // 0=unvisited,1=visiting,2=done
        Deque<String> stack = new ArrayDeque<String>();

        for (String e : reg.expressionNames()) state.put(e, 0);

        for (String e : reg.expressionNames()) {
            if (state.get(e) != null && state.get(e) == 0) dfs(e, r, state, stack);
        }

        Collections.reverse(r.topoOrder);
    }

    static void dfs(String node, GraphReport r, Map<String,Integer> state, Deque<String> stack) {
        state.put(node, 1);
        stack.push(node);

        Set<String> outs = r.exprToExpr.get(node);
        if (outs != null) {
            for (String nxt : outs) {
                Integer st = state.get(nxt);
                if (st == null) continue; // unknown already reported
                if (st == 0) {
                    dfs(nxt, r, state, stack);
                } else if (st == 1) {
                    // cycle from nxt back to nxt
                    List<String> cycle = new ArrayList<String>();
                    Iterator<String> it = stack.iterator();
                    while (it.hasNext()) {
                        String s = it.next();
                        cycle.add(s);
                        if (s.equals(nxt)) break;
                    }
                    Collections.reverse(cycle);
                    cycle.add(nxt);
                    r.cycles.add(cycle);
                }
            }
        }

        stack.pop();
        state.put(node, 2);
        r.topoOrder.add(node);
    }

    // =========================
    // UI Fields
    // =========================
    private final Registry reg = new Registry();

    // Variables tab
    private final VariablesTableModel fixedVarsModel = new VariablesTableModel(reg, new VariablesTableModel.Filter() {
        public boolean test(VarDef v) { return v.commonFixed; }
    });
    private final VariablesTableModel userVarsModel = new VariablesTableModel(reg, new VariablesTableModel.Filter() {
        public boolean test(VarDef v) { return !v.commonFixed; }
    });

    private final JTable fixedVarsTable = new JTable(fixedVarsModel);
    private final JTable userVarsTable = new JTable(userVarsModel);

    // Expressions tab
    private final DefaultListModel<String> exprListModel = new DefaultListModel<String>();
    private final JList<String> exprList = new JList<String>(exprListModel);

    // Compute output (shared)
    private final JTextArea computeOut = new JTextArea(12, 80);

    public ExpressionStudioSwing() {
        super("Expression Studio (Fixed Vars + Dialog Builder + Pickers + Shortcuts + Graph)");

        seedDefaults();          // user vars + expressions
        injectFixedVariables();  // common fixed vars

        setJMenuBar(buildMenu());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Variables", buildVariablesPanel());
        tabs.addTab("Expressions", buildExpressionsPanel());
        add(tabs);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1350, 900);
        setLocationRelativeTo(null);
    }

    // =========================
    // Seed defaults
    // =========================
    private void seedDefaults() {
        reg.vars.put("RSI_1H", new VarDef("RSI_1H", ValueType.NUMBER, VarValueMode.LITERAL, "28", "", null, false, false));
        reg.vars.put("ZScore_4H", new VarDef("ZScore_4H", ValueType.NUMBER, VarValueMode.LITERAL, "2.8", "", null, false, false));
        reg.vars.put("Today", new VarDef("Today", ValueType.DATE, VarValueMode.LITERAL, LocalDate.now().toString(), "", null, false, false));
        reg.vars.put("NowTime", new VarDef("NowTime", ValueType.TIME, VarValueMode.LITERAL, LocalTime.now().withNano(0).toString(), "", null, false, false));
        reg.vars.put("NowDT", new VarDef("NowDT", ValueType.DATETIME, VarValueMode.LITERAL, LocalDateTime.now().withNano(0).toString(), "", null, false, false));

        // entrySignal = (RSI_1H < 30) AND (ZScore_4H > 2.5)
        BoolGroup entry = new BoolGroup(JoinOp.AND);
        entry.children.add(new Compare(CmpOp.LT, new VarRef("RSI_1H"), new Literal(ValueType.NUMBER, "30")));
        entry.children.add(new Compare(CmpOp.GT, new VarRef("ZScore_4H"), new Literal(ValueType.NUMBER, "2.5")));
        reg.expressions.put("entrySignal", entry);

        // sessionGate = betweenTime(NowDT, "09:30", "16:00") AND NOT isWeekend(Today)
        BoolGroup sessionGate = new BoolGroup(JoinOp.AND);
        FuncCall between = new FuncCall(Func.BETWEEN_TIME);
        between.args.set(0, new VarRef("NowDT"));
        between.args.set(1, new Literal(ValueType.STRING, "09:30"));
        between.args.set(2, new Literal(ValueType.STRING, "16:00"));

        FuncCall weekend = new FuncCall(Func.IS_WEEKEND);
        weekend.args.set(0, new VarRef("Today"));

        sessionGate.children.add(between);
        sessionGate.children.add(new Unary(UnaryOp.NOT, weekend));
        reg.expressions.put("sessionGate", sessionGate);

        // score = abs(ZScore_4H) + (30 - RSI_1H)
        FuncCall abs = new FuncCall(Func.ABS); abs.args.set(0, new VarRef("ZScore_4H"));
        BinaryArith score = new BinaryArith(ArithOp.ADD, abs,
                new BinaryArith(ArithOp.SUB, new Literal(ValueType.NUMBER, "30"), new VarRef("RSI_1H")));
        reg.expressions.put("score", score);

        // Variable using expression result:
        reg.vars.put("ScoreVar", new VarDef("ScoreVar", ValueType.NUMBER, VarValueMode.EXPRESSION_REF, "", "score", null, false, false));

        // Variable using INLINE_EXPRESSION:
        // inline = minute(NowTime) >= 30  (BOOLEAN)
        FuncCall minute = new FuncCall(Func.MINUTE); minute.args.set(0, new VarRef("NowTime"));
        Compare inlineBool = new Compare(CmpOp.GE, minute, new Literal(ValueType.NUMBER, "30"));
        reg.vars.put("IsHalfPast", new VarDef("IsHalfPast", ValueType.BOOLEAN, VarValueMode.INLINE_EXPRESSION, "", "", inlineBool, false, false));

        reg.currentExpressionName = "entrySignal";
        refreshExpressionList();
    }

    // Common fixed variables injected (cannot be modified)
    private void injectFixedVariables() {
        // Examples: you can “pass” these from your main app context
        reg.vars.put("AccountBalance", new VarDef(
                "AccountBalance", ValueType.NUMBER, VarValueMode.LITERAL, "100000", "", null,
                true, true
        ));
        reg.vars.put("Broker", new VarDef(
                "Broker", ValueType.STRING, VarValueMode.LITERAL, "ALPACA", "", null,
                true, true
        ));
        reg.vars.put("Timezone", new VarDef(
                "Timezone", ValueType.STRING, VarValueMode.LITERAL, ZoneId.systemDefault().getId(), "", null,
                true, true
        ));

        // refresh tables
        fixedVarsModel.fireTableDataChanged();
        userVarsModel.fireTableDataChanged();
    }

    // =========================
    // Menu
    // =========================
    private JMenuBar buildMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save JSON...");
        JMenuItem load = new JMenuItem("Load JSON...");
        save.addActionListener(e -> onSaveJson());
        load.addActionListener(e -> onLoadJson());
        file.add(save);
        file.add(load);

        JMenu tools = new JMenu("Tools");
        JMenuItem compute = new JMenuItem("Compute Current Expression");
        compute.addActionListener(e -> onComputeCurrentExpression());

        JMenuItem graph = new JMenuItem("Validation Graph...");
        graph.addActionListener(e -> showGraphDialog());

        tools.add(compute);
        tools.add(graph);

        mb.add(file);
        mb.add(tools);
        return mb;
    }

    // =========================
    // Panels
    // =========================
    private JComponent buildVariablesPanel() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        // Fixed vars table
        fixedVarsTable.setFillsViewportHeight(true);
        fixedVarsTable.setRowHeight(24);

        JPanel fixedPanel = new JPanel(new BorderLayout(8,8));
        fixedPanel.add(new JLabel("Common Fixed Variables (read-only):"), BorderLayout.NORTH);
        fixedPanel.add(new JScrollPane(fixedVarsTable), BorderLayout.CENTER);

        // User vars table + CRUD buttons
        userVarsTable.setFillsViewportHeight(true);
        userVarsTable.setRowHeight(24);
        userVarsTable.setDefaultEditor(ValueType.class, new DefaultCellEditor(new JComboBox<ValueType>(ValueType.values())));
        userVarsTable.setDefaultEditor(VarValueMode.class, new DefaultCellEditor(new JComboBox<VarValueMode>(VarValueMode.values())));

        JPanel userPanel = new JPanel(new BorderLayout(8,8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton rename = new JButton("Rename");
        JButton del = new JButton("Delete");

        add.addActionListener(e -> onAddUserVariable());
        edit.addActionListener(e -> onEditUserVariable());
        rename.addActionListener(e -> onRenameUserVariable());
        del.addActionListener(e -> onDeleteUserVariable());

        buttons.add(add);
        buttons.add(edit);
        buttons.add(rename);
        buttons.add(del);

        userPanel.add(new JLabel("User Variables:"), BorderLayout.NORTH);
        userPanel.add(new JScrollPane(userVarsTable), BorderLayout.CENTER);
        userPanel.add(buttons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fixedPanel, userPanel);
        split.setResizeWeight(0.35);

        root.add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: Shortcuts: betweenTime(NowDT,\"09:30\",\"16:00\"), isWeekend(Today), inDateRange(Today,\"yyyy-MM-dd\",\"yyyy-MM-dd\")");
        root.add(hint, BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildExpressionsPanel() {
        JPanel p = new JPanel(new BorderLayout(10,10));
        p.setBorder(new EmptyBorder(10,10,10,10));

        exprList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit...");
        JButton setCurrent = new JButton("Set Current");
        JButton rename = new JButton("Rename");
        JButton dup = new JButton("Duplicate");
        JButton del = new JButton("Delete");
        JButton compute = new JButton("Compute Current");

        add.addActionListener(e -> onAddExpression());
        edit.addActionListener(e -> onEditExpression());
        setCurrent.addActionListener(e -> onSetCurrentExpression());
        rename.addActionListener(e -> onRenameExpression());
        dup.addActionListener(e -> onDuplicateExpression());
        del.addActionListener(e -> onDeleteExpression());
        compute.addActionListener(e -> onComputeCurrentExpression());

        buttons.add(add);
        buttons.add(edit);
        buttons.add(setCurrent);
        buttons.add(rename);
        buttons.add(dup);
        buttons.add(del);
        buttons.add(new JSeparator(SwingConstants.VERTICAL));
        buttons.add(compute);

        computeOut.setEditable(false);
        computeOut.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        computeOut.setLineWrap(true);
        computeOut.setWrapStyleWord(true);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(exprList), new JScrollPane(computeOut));
        split.setResizeWeight(0.45);

        p.add(buttons, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    // =========================
    // Expressions CRUD
    // =========================
    private void refreshExpressionList() {
        exprListModel.clear();
        for (String k : reg.expressionNames()) exprListModel.addElement(k);
        if (!safe(reg.currentExpressionName).isEmpty()) exprList.setSelectedValue(reg.currentExpressionName, true);
    }

    private void onAddExpression() {
        String name = JOptionPane.showInputDialog(this, "Expression name:");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) return;
        if (reg.expressions.containsKey(name)) {
            JOptionPane.showMessageDialog(this, "Expression already exists: " + name);
            return;
        }

        Expr root = new Compare(CmpOp.GT, new VarRef(firstVarOr("A")), new Literal(ValueType.NUMBER, "0"));
        reg.expressions.put(name, root);
        reg.currentExpressionName = name;

        refreshExpressionList();
    }

    private void onEditExpression() {
        String sel = exprList.getSelectedValue();
        if (sel == null) return;

        Expr original = reg.expressions.get(sel);
        ExprEditorDialog dlg = new ExprEditorDialog(this, reg, original, "Edit Expression: " + sel);
        dlg.setVisible(true);

        if (dlg.isSaved()) {
            reg.expressions.put(sel, dlg.getResultExpr());
            // keep current expression name unchanged, unless none
            if (safe(reg.currentExpressionName).isEmpty()) reg.currentExpressionName = sel;
        }
    }

    private void onSetCurrentExpression() {
        String sel = exprList.getSelectedValue();
        if (sel == null) return;
        reg.currentExpressionName = sel;
        refreshExpressionList();
    }

    private void onRenameExpression() {
        String oldName = exprList.getSelectedValue();
        if (oldName == null) return;

        String newName = JOptionPane.showInputDialog(this, "New expression name:", oldName);
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty()) return;

        if (reg.expressions.containsKey(newName)) {
            JOptionPane.showMessageDialog(this, "Expression already exists: " + newName);
            return;
        }

        Expr e = reg.expressions.remove(oldName);
        if (e == null) return;
        reg.expressions.put(newName, e);

        if (oldName.equals(reg.currentExpressionName)) reg.currentExpressionName = newName;

        // Update ExprRef nodes in all expressions
        for (Expr ex : reg.expressions.values()) ExprWalker.renameExprRefs(ex, oldName, newName);

        // Update variables that reference expressions (expr-ref mode)
        for (VarDef v : reg.vars.values()) {
            if (v.mode == VarValueMode.EXPRESSION_REF && oldName.equals(v.expressionName)) v.expressionName = newName;
            if (v.mode == VarValueMode.INLINE_EXPRESSION && v.inlineExpr != null) ExprWalker.renameExprRefs(v.inlineExpr, oldName, newName);
        }

        refreshExpressionList();
    }

    private void onDuplicateExpression() {
        String sel = exprList.getSelectedValue();
        if (sel == null) return;

        String newName = JOptionPane.showInputDialog(this, "Duplicate name:", sel + "_copy");
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty()) return;

        if (reg.expressions.containsKey(newName)) {
            JOptionPane.showMessageDialog(this, "Expression already exists: " + newName);
            return;
        }

        try {
            ObjectMapper m = new ObjectMapper().registerModule(new JavaTimeModule());
            String json = m.writeValueAsString(reg.expressions.get(sel));
            Expr cloned = m.readValue(json, Expr.class);

            reg.expressions.put(newName, cloned);
            refreshExpressionList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Duplicate failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteExpression() {
        String sel = exprList.getSelectedValue();
        if (sel == null) return;

        if (JOptionPane.showConfirmDialog(this, "Delete expression '" + sel + "'?", "Confirm",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        reg.expressions.remove(sel);

        if (sel.equals(reg.currentExpressionName)) {
            reg.currentExpressionName = reg.expressionNames().isEmpty() ? "" : reg.expressionNames().get(0);
        }

        refreshExpressionList();
    }

    // =========================
    // Compute + validation
    // =========================
    private void onComputeCurrentExpression() {
        String exprName = safe(reg.currentExpressionName);
        if (exprName.isEmpty() || !reg.expressions.containsKey(exprName)) {
            JOptionPane.showMessageDialog(this, "No current expression selected.");
            return;
        }

        List<String> missing = validateAllReferences();
        if (!missing.isEmpty()) {
            computeOut.setText("Missing references:\n" + join(missing, "\n"));
            return;
        }

        Expr root = reg.expressions.get(exprName);
        StringBuilder trace = new StringBuilder();
        try {
            Object out = root.eval(new EvalContext(reg), trace);
            computeOut.setText(
                    "Current Expression: " + exprName + "\n" +
                            "Text: " + root.toExprString() + "\n" +
                            "Result Type: " + root.type(reg) + "\n" +
                            "Result: " + out + "\n\n" +
                            "Trace:\n" + trace
            );
        } catch (Exception ex) {
            computeOut.setText("ERROR: " + ex.getMessage() + "\n\nTrace:\n" + trace);
        }
    }

    private List<String> validateAllReferences() {
        List<String> errors = new ArrayList<String>();

        // Validate expression refs/var refs from each expression
        for (Map.Entry<String, Expr> en : reg.expressions.entrySet()) {
            String exprName = en.getKey();
            Expr root = en.getValue();
            Set<String> varRefs = new LinkedHashSet<String>();
            Set<String> exprRefs = new LinkedHashSet<String>();
            root.collectVarRefs(varRefs);
            root.collectExprRefs(exprRefs);

            for (String v : varRefs) if (!reg.vars.containsKey(v)) errors.add("Expression '" + exprName + "' missing variable: " + v);
            for (String e : exprRefs) if (!reg.expressions.containsKey(e)) errors.add("Expression '" + exprName + "' missing expression: " + e);
        }

        // Validate variables (expr-ref and inline)
        for (VarDef v : reg.vars.values()) {
            if (v.mode == VarValueMode.EXPRESSION_REF) {
                String ref = safe(v.expressionName);
                if (ref.isEmpty() || !reg.expressions.containsKey(ref)) errors.add("Variable '" + v.name + "' missing expression ref: " + ref);
            } else if (v.mode == VarValueMode.INLINE_EXPRESSION) {
                if (v.inlineExpr == null) {
                    errors.add("Variable '" + v.name + "' inline expression is null");
                } else {
                    Set<String> varRefs = new LinkedHashSet<String>();
                    Set<String> exprRefs = new LinkedHashSet<String>();
                    v.inlineExpr.collectVarRefs(varRefs);
                    v.inlineExpr.collectExprRefs(exprRefs);
                    for (String vr : varRefs) if (!reg.vars.containsKey(vr)) errors.add("Variable '" + v.name + "' inline missing variable: " + vr);
                    for (String er : exprRefs) if (!reg.expressions.containsKey(er)) errors.add("Variable '" + v.name + "' inline missing expression: " + er);
                }
            }
        }

        return errors;
    }

    private void showGraphDialog() {
        GraphReport gr = buildGraph(reg);

        JTextArea out = new JTextArea(30, 95);
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        out.setEditable(false);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Expression -> Expression ===\n");
        for (Map.Entry<String, Set<String>> e : gr.exprToExpr.entrySet())
            sb.append(e.getKey()).append(" => ").append(e.getValue()).append("\n");

        sb.append("\n=== Expression -> Variables ===\n");
        for (Map.Entry<String, Set<String>> e : gr.exprToVar.entrySet())
            sb.append(e.getKey()).append(" => ").append(e.getValue()).append("\n");

        sb.append("\n=== Variable -> Expression (expr-ref + inline expr refs) ===\n");
        for (Map.Entry<String, Set<String>> v : gr.varToExpr.entrySet())
            if (!v.getValue().isEmpty())
                sb.append(v.getKey()).append(" => ").append(v.getValue()).append("\n");

        sb.append("\n=== Missing Vars ===\n");
        if (gr.missingVars.isEmpty()) sb.append("(none)\n");
        else for (String m : gr.missingVars) sb.append(m).append("\n");

        sb.append("\n=== Missing Exprs ===\n");
        if (gr.missingExprs.isEmpty()) sb.append("(none)\n");
        else for (String m : gr.missingExprs) sb.append(m).append("\n");

        sb.append("\n=== Cycles (Expr->Expr) ===\n");
        if (gr.cycles.isEmpty()) sb.append("(none)\n");
        else for (List<String> c : gr.cycles) sb.append(c).append("\n");

        sb.append("\n=== Topological Order (Expr->Expr) ===\n");
        sb.append(gr.topoOrder).append("\n");

        out.setText(sb.toString());

        JDialog dlg = new JDialog(this, "Validation Graph", true);
        dlg.setLayout(new BorderLayout(8,8));
        dlg.add(new JScrollPane(out), BorderLayout.CENTER);

        JButton copy = new JButton("Copy");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(out.getText()), null);
            JOptionPane.showMessageDialog(dlg, "Copied.");
        });

        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.setVisible(false));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(copy);
        south.add(close);
        dlg.add(south, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // =========================
    // Save / Load JSON
    // =========================
    private void onSaveJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save JSON");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            JsonObjectMapper.save(fc.getSelectedFile(), reg);
            JOptionPane.showMessageDialog(this, "Saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoadJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load JSON");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Registry loaded = JsonObjectMapper.load(fc.getSelectedFile());

            // preserve fixed/common vars currently injected? You can choose.
            // Here: we load everything from file, THEN re-inject fixed vars (overriding).
            reg.vars.clear();
            reg.expressions.clear();

            if (loaded.vars != null) reg.vars.putAll(loaded.vars);
            if (loaded.expressions != null) reg.expressions.putAll(loaded.expressions);
            reg.currentExpressionName = loaded.currentExpressionName == null ? "" : loaded.currentExpressionName;

            // Re-inject fixed vars (ensures they exist & remain immutable)
            injectFixedVariables();

            fixedVarsModel.fireTableDataChanged();
            userVarsModel.fireTableDataChanged();

            refreshExpressionList();

            JOptionPane.showMessageDialog(this, "Loaded.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    // Variables CRUD (User only)
    // =========================
    private void onAddUserVariable() {
        VarDef created = variableDialog(null);
        if (created == null) return;
        if (reg.vars.containsKey(created.name)) {
            JOptionPane.showMessageDialog(this, "Variable already exists: " + created.name);
            return;
        }
        reg.vars.put(created.name, created);
        fixedVarsModel.fireTableDataChanged();
        userVarsModel.fireTableDataChanged();
    }

    private void onEditUserVariable() {
        VarDef v = selectedUserVar();
        if (v == null) return;
        if (v.readOnly) { JOptionPane.showMessageDialog(this, "This variable is read-only."); return; }

        VarDef updated = variableDialog(v);
        if (updated == null) return;

        // name not editable in edit dialog
        v.type = updated.type;
        v.mode = updated.mode;
        v.literalValue = updated.literalValue;
        v.expressionName = updated.expressionName;
        v.inlineExpr = updated.inlineExpr;

        fixedVarsModel.fireTableDataChanged();
        userVarsModel.fireTableDataChanged();
    }

    private void onRenameUserVariable() {
        VarDef v = selectedUserVar();
        if (v == null) return;
        if (v.readOnly) { JOptionPane.showMessageDialog(this, "This variable is read-only."); return; }

        String oldName = v.name;

        String newName = JOptionPane.showInputDialog(this, "New variable name:", oldName);
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty()) return;

        if (reg.vars.containsKey(newName)) {
            JOptionPane.showMessageDialog(this, "Variable already exists: " + newName);
            return;
        }

        VarDef removed = reg.vars.remove(oldName);
        removed.name = newName;
        reg.vars.put(newName, removed);

        // Update VarRef nodes in all expressions
        for (Expr e : reg.expressions.values()) ExprWalker.renameVarRefs(e, oldName, newName);

        // Update inline expressions of variables (including fixed ones)
        for (VarDef vv : reg.vars.values()) {
            if (vv.mode == VarValueMode.INLINE_EXPRESSION && vv.inlineExpr != null) {
                ExprWalker.renameVarRefs(vv.inlineExpr, oldName, newName);
            }
        }

        fixedVarsModel.fireTableDataChanged();
        userVarsModel.fireTableDataChanged();
    }

    private void onDeleteUserVariable() {
        VarDef v = selectedUserVar();
        if (v == null) return;
        if (v.readOnly) { JOptionPane.showMessageDialog(this, "This variable is read-only."); return; }

        if (JOptionPane.showConfirmDialog(this, "Delete variable '" + v.name + "'?", "Confirm",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        reg.vars.remove(v.name);
        fixedVarsModel.fireTableDataChanged();
        userVarsModel.fireTableDataChanged();
    }

    private VarDef selectedUserVar() {
        int row = userVarsTable.getSelectedRow();
        if (row < 0) return null;
        return userVarsModel.getVarAt(row);
    }

    // =========================
    // Variable dialog (with pickers + inline AST editor)
    // =========================
    private VarDef variableDialog(VarDef current) {
        boolean isEdit = (current != null);

        JTextField name = new JTextField(isEdit ? current.name : "");
        name.setEnabled(!isEdit);

        JComboBox<ValueType> type = new JComboBox<ValueType>(ValueType.values());
        type.setSelectedItem(isEdit ? current.type : ValueType.NUMBER);

        JComboBox<VarValueMode> mode = new JComboBox<VarValueMode>(VarValueMode.values());
        mode.setSelectedItem(isEdit ? current.mode : VarValueMode.LITERAL);

        // literal editor with picker cards
        LiteralValueEditor literalEditor = new LiteralValueEditor();
        if (isEdit) literalEditor.setTypeAndValue(current.type, safe(current.literalValue));
        else literalEditor.setTypeAndValue((ValueType) type.getSelectedItem(), "");

        // expression ref dropdown
        JComboBox<String> exprRef = new JComboBox<String>();
        exprRef.removeAllItems();
        for (String e : reg.expressionNames()) exprRef.addItem(e);
        if (isEdit) exprRef.setSelectedItem(safe(current.expressionName));

        // inline expression editor
        JButton editInlineBtn = new JButton("Edit Inline Expression...");
        JLabel inlineStatus = new JLabel(isEdit && current.inlineExpr != null ? "Inline AST: OK" : "Inline AST: (none)");
        final Expr[] inlineHolder = new Expr[1];
        inlineHolder[0] = isEdit ? current.inlineExpr : null;

        editInlineBtn.addActionListener(e -> {
            Expr start = inlineHolder[0];
            ExprEditorDialog dlg = new ExprEditorDialog(this, reg, start, "Edit Inline Expression for Variable");
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                inlineHolder[0] = dlg.getResultExpr();
                inlineStatus.setText(inlineHolder[0] != null ? "Inline AST: OK" : "Inline AST: (none)");
            }
        });

        // wire type -> picker update
        type.addActionListener(e -> {
            ValueType t = (ValueType) type.getSelectedItem();
            literalEditor.setTypeAndValue(t, literalEditor.getValueAsString(t));
        });

        Runnable updateModeUI = new Runnable() {
            public void run() {
                VarValueMode m = (VarValueMode) mode.getSelectedItem();
                boolean isLiteral = (m == VarValueMode.LITERAL);
                boolean isExprRef = (m == VarValueMode.EXPRESSION_REF);
                boolean isInline = (m == VarValueMode.INLINE_EXPRESSION);

                literalEditor.setEnabledAll(isLiteral);
                exprRef.setEnabled(isExprRef);
                editInlineBtn.setEnabled(isInline);
            }
        };
        mode.addActionListener(e -> updateModeUI.run());
        updateModeUI.run();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(labeled("Name:", name));
        panel.add(labeled("Type:", type));
        panel.add(labeled("Mode:", mode));
        panel.add(new JSeparator());

        panel.add(new JLabel("Literal Value (if LITERAL):"));
        panel.add(literalEditor.panel);

        panel.add(Box.createVerticalStrut(8));
        panel.add(labeled("Expression (if EXPRESSION_REF):", exprRef));

        JPanel inlineRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inlineRow.add(editInlineBtn);
        inlineRow.add(inlineStatus);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Inline Expression (if INLINE_EXPRESSION):"));
        panel.add(inlineRow);

        int ok = JOptionPane.showConfirmDialog(this, panel, isEdit ? "Edit Variable" : "Add Variable",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;

        String n = name.getText().trim();
        if (n.isEmpty()) { JOptionPane.showMessageDialog(this, "Name required."); return null; }

        VarDef out = new VarDef();
        out.name = n;
        out.type = (ValueType) type.getSelectedItem();
        out.mode = (VarValueMode) mode.getSelectedItem();

        out.readOnly = false;
        out.commonFixed = false;

        out.literalValue = literalEditor.getValueAsString(out.type);

        Object sel = exprRef.getSelectedItem();
        out.expressionName = sel == null ? "" : sel.toString();

        out.inlineExpr = inlineHolder[0];

        if (out.mode == VarValueMode.INLINE_EXPRESSION && out.inlineExpr == null) {
            JOptionPane.showMessageDialog(this, "INLINE_EXPRESSION mode requires an inline AST. Click 'Edit Inline Expression...' and save.");
            return null;
        }

        return out;
    }

    // =========================
    // Expression Builder Dialog (used for expressions + inline AST)
    // =========================
    static class ExprEditorDialog extends JDialog {
        private boolean saved = false;
        private Expr resultExpr;

        private final Registry reg;

        private DefaultTreeModel model;
        private final JTree tree = new JTree();

        private final CardLayout editorCards = new CardLayout();
        private final JPanel editorPanel = new JPanel(editorCards);
        private boolean updating = false;

        private final JComboBox<String> varDropdown = new JComboBox<String>();
        private final JComboBox<String> exprDropdown = new JComboBox<String>();

        private final JComboBox<ValueType> litType = new JComboBox<ValueType>(ValueType.values());
        private final LiteralValueEditor literalEditor = new LiteralValueEditor();

        private final JComboBox<UnaryOp> unaryOp = new JComboBox<UnaryOp>(UnaryOp.values());
        private final JComboBox<ArithOp> arithOp = new JComboBox<ArithOp>(ArithOp.values());
        private final JComboBox<Func> funcOp = new JComboBox<Func>(Func.values());
        private final JComboBox<CmpOp> cmpOp = new JComboBox<CmpOp>(CmpOp.values());
        private final JRadioButton bgAnd = new JRadioButton("AND");
        private final JRadioButton bgOr = new JRadioButton("OR");

        private final JTextArea preview = new JTextArea(5, 70);

        ExprEditorDialog(Frame owner, Registry reg, Expr start, String title) {
            super(owner, title, true);
            this.reg = reg;

            Expr root = (start == null)
                    ? new Compare(CmpOp.GT, new VarRef(first(reg.variableNames(), "A")), new Literal(ValueType.NUMBER, "0"))
                    : start;
            this.resultExpr = root;

            setLayout(new BorderLayout(8,8));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

            JButton addChild = new JButton("Add Child");
            JButton replace = new JButton("Replace Node");
            JButton remove = new JButton("Remove Node");
            JButton copy = new JButton("Copy Text");
            JButton ok = new JButton("Save");
            JButton cancel = new JButton("Cancel");

            addChild.addActionListener(e -> onAddChild());
            replace.addActionListener(e -> onReplaceNode());
            remove.addActionListener(e -> onRemoveNode());
            copy.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(preview.getText()), null);
                JOptionPane.showMessageDialog(this, "Copied.");
            });

            ok.addActionListener(e -> {
                saved = true;
                resultExpr = ((ExprTreeNode) model.getRoot()).expr();
                setVisible(false);
            });
            cancel.addActionListener(e -> {
                saved = false;
                setVisible(false);
            });

            top.add(addChild);
            top.add(replace);
            top.add(remove);
            top.add(copy);
            top.add(new JSeparator(SwingConstants.VERTICAL));
            top.add(ok);
            top.add(cancel);

            add(top, BorderLayout.NORTH);

            tree.setRootVisible(true);
            tree.setShowsRootHandles(true);
            tree.setCellRenderer(new ExprRenderer());

            ExprTreeNode rootNode = new ExprTreeNode(root);
            buildChildren(rootNode);
            model = new DefaultTreeModel(rootNode);
            tree.setModel(model);
            tree.expandRow(0);

            buildEditorCards();
            wireEditors();

            tree.addTreeSelectionListener(e -> onSelect());

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), new JScrollPane(editorPanel));
            split.setResizeWeight(0.45);
            add(split, BorderLayout.CENTER);

            preview.setEditable(false);
            preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            preview.setLineWrap(true);
            preview.setWrapStyleWord(true);

            JPanel bottom = new JPanel(new BorderLayout(6,6));
            bottom.add(new JLabel("Preview:"), BorderLayout.NORTH);
            bottom.add(new JScrollPane(preview), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            refreshDropdowns();
            refreshPreview();

            setSize(1200, 780);
            setLocationRelativeTo(owner);
        }

        boolean isSaved(){ return saved; }
        Expr getResultExpr(){ return resultExpr; }

        private void refreshDropdowns() {
            updating = true;
            try {
                Object curVar = varDropdown.getSelectedItem();
                varDropdown.removeAllItems();
                for (String v : reg.variableNames()) varDropdown.addItem(v);
                if (curVar != null) varDropdown.setSelectedItem(curVar);

                Object curExpr = exprDropdown.getSelectedItem();
                exprDropdown.removeAllItems();
                for (String e : reg.expressionNames()) exprDropdown.addItem(e);
                if (curExpr != null) exprDropdown.setSelectedItem(curExpr);
            } finally {
                updating = false;
            }
        }

        private void buildChildren(ExprTreeNode node) {
            node.removeAllChildren();
            Expr e = node.expr();

            if (e instanceof Unary) {
                Unary u = (Unary) e;
                ExprTreeNode c = new ExprTreeNode(u.child);
                node.add(c);
                buildChildren(c);
            } else if (e instanceof BinaryArith) {
                BinaryArith b = (BinaryArith) e;
                ExprTreeNode l = new ExprTreeNode(b.left);
                ExprTreeNode r = new ExprTreeNode(b.right);
                node.add(l); node.add(r);
                buildChildren(l); buildChildren(r);
            } else if (e instanceof FuncCall) {
                FuncCall f = (FuncCall) e;
                for (Expr a : f.args) {
                    ExprTreeNode c = new ExprTreeNode(a);
                    node.add(c);
                    buildChildren(c);
                }
            } else if (e instanceof Compare) {
                Compare c = (Compare) e;
                ExprTreeNode l = new ExprTreeNode(c.left);
                ExprTreeNode r = new ExprTreeNode(c.right);
                node.add(l); node.add(r);
                buildChildren(l); buildChildren(r);
            } else if (e instanceof BoolGroup) {
                BoolGroup g = (BoolGroup) e;
                for (Expr child : g.children) {
                    ExprTreeNode c = new ExprTreeNode(child);
                    node.add(c);
                    buildChildren(c);
                }
            }
        }

        private ExprTreeNode selectedNode() {
            TreePath p = tree.getSelectionPath();
            return p == null ? null : (ExprTreeNode) p.getLastPathComponent();
        }

        private void refreshPreview() {
            ExprTreeNode root = (ExprTreeNode) model.getRoot();
            preview.setText(root.expr().toExprString());
        }

        private void buildEditorCards() {
            editorPanel.removeAll();
            editorPanel.add(new JLabel("Select a node to edit."), "EMPTY");
            editorPanel.add(card("Variable", labeled("Name:", varDropdown)), "VAR");
            editorPanel.add(card("Expression Ref", labeled("Name:", exprDropdown)), "REF");

            JPanel litCard = card("Literal",
                    labeled("Type:", litType),
                    new JLabel("Value:"),
                    literalEditor.panel
            );
            editorPanel.add(litCard, "LIT");

            editorPanel.add(card("Unary", labeled("Op:", unaryOp)), "UNARY");
            editorPanel.add(card("Arithmetic", labeled("Op:", arithOp)), "ARITH");
            editorPanel.add(card("Function", labeled("Func:", funcOp),
                    new JLabel("Shortcuts: minutesOfDay, betweenTime, isWeekend, inDateRange")), "FUNC");
            editorPanel.add(card("Comparison", labeled("Cmp:", cmpOp)), "CMP");
            ButtonGroup bg = new ButtonGroup(); bg.add(bgAnd); bg.add(bgOr);
            editorPanel.add(card("Bool Group", row(new JLabel("Join:"), bgAnd, bgOr)), "BOOL");
            editorCards.show(editorPanel, "EMPTY");
        }

        private void wireEditors() {
            varDropdown.addActionListener(e -> pushEdits());
            exprDropdown.addActionListener(e -> pushEdits());

            litType.addActionListener(e -> {
                ValueType t = (ValueType) litType.getSelectedItem();
                literalEditor.setTypeAndValue(t, literalEditor.getValueAsString(t));
                pushEdits();
            });
            literalEditor.addChangeListener(new Runnable() {
                public void run() { pushEdits(); }
            });

            unaryOp.addActionListener(e -> pushEdits());
            arithOp.addActionListener(e -> pushEdits());
            funcOp.addActionListener(e -> {
                pushEdits(); // will adjust arity
            });
            cmpOp.addActionListener(e -> pushEdits());
            bgAnd.addActionListener(e -> pushEdits());
            bgOr.addActionListener(e -> pushEdits());
        }

        private void onSelect() {
            ExprTreeNode n = selectedNode();
            if (n == null) { editorCards.show(editorPanel, "EMPTY"); return; }

            updating = true;
            try {
                Expr e = n.expr();
                if (e instanceof VarRef) {
                    editorCards.show(editorPanel, "VAR");
                    varDropdown.setSelectedItem(((VarRef) e).name);
                } else if (e instanceof ExprRef) {
                    editorCards.show(editorPanel, "REF");
                    exprDropdown.setSelectedItem(((ExprRef) e).exprName);
                } else if (e instanceof Literal) {
                    editorCards.show(editorPanel, "LIT");
                    Literal l = (Literal) e;
                    litType.setSelectedItem(l.litType);
                    literalEditor.setTypeAndValue(l.litType, l.value);
                } else if (e instanceof Unary) {
                    editorCards.show(editorPanel, "UNARY");
                    unaryOp.setSelectedItem(((Unary) e).op);
                } else if (e instanceof BinaryArith) {
                    editorCards.show(editorPanel, "ARITH");
                    arithOp.setSelectedItem(((BinaryArith) e).op);
                } else if (e instanceof FuncCall) {
                    editorCards.show(editorPanel, "FUNC");
                    funcOp.setSelectedItem(((FuncCall) e).func);
                } else if (e instanceof Compare) {
                    editorCards.show(editorPanel, "CMP");
                    cmpOp.setSelectedItem(((Compare) e).op);
                } else if (e instanceof BoolGroup) {
                    editorCards.show(editorPanel, "BOOL");
                    BoolGroup g = (BoolGroup) e;
                    if (g.op == JoinOp.AND) bgAnd.setSelected(true); else bgOr.setSelected(true);
                } else editorCards.show(editorPanel, "EMPTY");
            } finally {
                updating = false;
            }
            refreshPreview();
        }

        private void pushEdits() {
            if (updating) return;
            ExprTreeNode n = selectedNode();
            if (n == null) return;

            Expr e = n.expr();

            if (e instanceof VarRef) {
                Object sel = varDropdown.getSelectedItem();
                if (sel != null) ((VarRef) e).name = sel.toString().trim();
            } else if (e instanceof ExprRef) {
                Object sel = exprDropdown.getSelectedItem();
                if (sel != null) ((ExprRef) e).exprName = sel.toString().trim();
            } else if (e instanceof Literal) {
                Literal l = (Literal) e;
                ValueType t = (ValueType) litType.getSelectedItem();
                l.litType = t;
                l.value = literalEditor.getValueAsString(t);
            } else if (e instanceof Unary) {
                ((Unary) e).op = (UnaryOp) unaryOp.getSelectedItem();
            } else if (e instanceof BinaryArith) {
                ((BinaryArith) e).op = (ArithOp) arithOp.getSelectedItem();
            } else if (e instanceof FuncCall) {
                FuncCall f = (FuncCall) e;
                Func chosen = (Func) funcOp.getSelectedItem();
                if (chosen != null) {
                    f.setFunc(chosen);
                    buildChildren(n);
                    model.reload(n);
                }
            } else if (e instanceof Compare) {
                ((Compare) e).op = (CmpOp) cmpOp.getSelectedItem();
            } else if (e instanceof BoolGroup) {
                ((BoolGroup) e).op = bgAnd.isSelected() ? JoinOp.AND : JoinOp.OR;
            }

            model.nodeChanged(n);
            refreshPreview();
        }

        private void onAddChild() {
            ExprTreeNode parent = selectedNode();
            if (parent == null) parent = (ExprTreeNode) model.getRoot();

            Expr pe = parent.expr();
            if (pe instanceof BoolGroup) {
                BoolGroup g = (BoolGroup) pe;
                Expr child = new Compare(CmpOp.GT, new VarRef(first(reg.variableNames(), "A")), new Literal(ValueType.NUMBER, "0"));
                g.children.add(child);

                ExprTreeNode cn = new ExprTreeNode(child);
                model.insertNodeInto(cn, parent, parent.getChildCount());
                buildChildren(cn);
                tree.expandPath(new TreePath(parent.getPath()));
                tree.setSelectionPath(new TreePath(cn.getPath()));
                refreshPreview();
                return;
            }
            if (pe instanceof Unary) {
                Unary u = (Unary) pe;
                u.child = (u.op == UnaryOp.NOT) ? new BoolGroup(JoinOp.AND) : new Literal(ValueType.NUMBER, "0");
                buildChildren(parent);
                model.reload(parent);
                refreshPreview();
                return;
            }
            JOptionPane.showMessageDialog(this, "Add Child works for BOOL GROUP or UNARY.");
        }

        private void onReplaceNode() {
            ExprTreeNode node = selectedNode();
            if (node == null) return;

            String[] choices = {"VAR_REF","EXPR_REF","LITERAL","ARITH","FUNC","COMPARE","BOOL_GROUP","UNARY_NEG","UNARY_NOT"};
            String choice = (String) JOptionPane.showInputDialog(this, "Replace with:", "Replace Node Type",
                    JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (choice == null) return;

            Expr newExpr;
            if ("VAR_REF".equals(choice)) newExpr = new VarRef(first(reg.variableNames(), "VAR"));
            else if ("EXPR_REF".equals(choice)) newExpr = new ExprRef(first(reg.expressionNames(), "EXPR"));
            else if ("LITERAL".equals(choice)) newExpr = new Literal(ValueType.NUMBER, "0");
            else if ("ARITH".equals(choice)) newExpr = new BinaryArith(ArithOp.ADD, new Literal(ValueType.NUMBER, "0"), new Literal(ValueType.NUMBER, "0"));
            else if ("FUNC".equals(choice)) newExpr = new FuncCall(Func.MINUTE);
            else if ("COMPARE".equals(choice)) newExpr = new Compare(CmpOp.GT, new VarRef(first(reg.variableNames(), "A")), new Literal(ValueType.NUMBER, "0"));
            else if ("BOOL_GROUP".equals(choice)) newExpr = new BoolGroup(JoinOp.AND);
            else if ("UNARY_NEG".equals(choice)) newExpr = new Unary(UnaryOp.NEG, new Literal(ValueType.NUMBER, "0"));
            else newExpr = new Unary(UnaryOp.NOT, new BoolGroup(JoinOp.AND));

            if (node == model.getRoot()) {
                node.setUserObject(newExpr);
            } else {
                ExprTreeNode parent = (ExprTreeNode) node.getParent();
                int idx = parent.getIndex(node);
                replaceChildExpr(parent.expr(), idx, newExpr);
                node.setUserObject(newExpr);
            }

            buildChildren(node);
            model.reload(node);
            tree.setSelectionPath(new TreePath(node.getPath()));
            refreshPreview();
            onSelect();
        }

        private void onRemoveNode() {
            ExprTreeNode node = selectedNode();
            if (node == null) return;
            if (node == model.getRoot()) {
                JOptionPane.showMessageDialog(this, "Cannot remove root.");
                return;
            }

            ExprTreeNode parent = (ExprTreeNode) node.getParent();
            int idx = parent.getIndex(node);

            if (parent.expr() instanceof BoolGroup) {
                ((BoolGroup) parent.expr()).children.remove(idx);
                model.removeNodeFromParent(node);
            } else {
                Expr replacement = new Literal(ValueType.NUMBER, "0");
                if (parent.expr() instanceof Compare && idx == 0) replacement = new VarRef(first(reg.variableNames(), "A"));
                replaceChildExpr(parent.expr(), idx, replacement);
                buildChildren(parent);
                model.reload(parent);
            }

            tree.setSelectionPath(new TreePath(parent.getPath()));
            refreshPreview();
        }

        private JPanel card(String title, JComponent... comps) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(new EmptyBorder(12,12,12,12));
            JLabel t = new JLabel(title);
            t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
            p.add(t);
            p.add(Box.createVerticalStrut(10));
            for (JComponent c : comps) { p.add(c); p.add(Box.createVerticalStrut(8)); }
            return p;
        }

        private JPanel labeled(String label, JComponent comp) {
            JPanel row = new JPanel(new BorderLayout(6,6));
            row.add(new JLabel(label), BorderLayout.WEST);
            row.add(comp, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            return row;
        }

        private JPanel row(JComponent... comps) {
            JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            for (JComponent c: comps) r.add(c);
            return r;
        }

        private static String first(List<String> items, String fallback) {
            return (items == null || items.isEmpty()) ? fallback : items.get(0);
        }
    }

    // =========================
    // Table model (filterable)
    // =========================
    static class VariablesTableModel extends AbstractTableModel {
        interface Filter { boolean test(VarDef v); }

        private final Registry reg;
        private final Filter filter;

        private final String[] cols = {"Name","Type","Mode","Value","ExprName","InlineExpr","ReadOnly","Common"};

        VariablesTableModel(Registry reg, Filter filter){
            this.reg = reg;
            this.filter = filter;
        }

        private List<VarDef> rows() {
            List<VarDef> out = new ArrayList<VarDef>();
            for (VarDef v : reg.vars.values()) if (filter.test(v)) out.add(v);
            return out;
        }

        public VarDef getVarAt(int row) { return rows().get(row); }

        public int getRowCount(){ return rows().size(); }
        public int getColumnCount(){ return cols.length; }
        public String getColumnName(int c){ return cols[c]; }

        public Object getValueAt(int row, int col) {
            VarDef v = rows().get(row);
            if (col == 0) return v.name;
            if (col == 1) return v.type;
            if (col == 2) return v.mode;
            if (col == 3) return v.literalValue;
            if (col == 4) return v.expressionName;
            if (col == 5) return v.inlineExpr == null ? "" : v.inlineExpr.toExprString();
            if (col == 6) return v.readOnly;
            return v.commonFixed;
        }

        public boolean isCellEditable(int row, int col) {
            VarDef v = rows().get(row);
            if (v.readOnly) return false;
            // keep editing in table minimal; main edits via dialog
            return col == 1 || col == 2 || col == 3 || col == 4;
        }

        public void setValueAt(Object aValue, int row, int col) {
            VarDef v = rows().get(row);
            if (v.readOnly) return;

            if (col == 1) v.type = (ValueType) aValue;
            else if (col == 2) v.mode = (VarValueMode) aValue;
            else if (col == 3) v.literalValue = aValue == null ? "" : aValue.toString();
            else if (col == 4) v.expressionName = aValue == null ? "" : aValue.toString();
            fireTableRowsUpdated(row, row);
        }

        public Class<?> getColumnClass(int c) {
            if (c == 1) return ValueType.class;
            if (c == 2) return VarValueMode.class;
            if (c == 6 || c == 7) return Boolean.class;
            return String.class;
        }
    }

    // =========================
    // Tree renderer
    // =========================
    static class ExprRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof ExprTreeNode) {
                ExprTreeNode n = (ExprTreeNode) value;
                setText(n.expr().displayName());
            }
            return this;
        }
    }

    // =========================
    // Literal value editor (Text + Date/Time/DateTime pickers)
    // =========================
    static class LiteralValueEditor {
        final JPanel panel = new JPanel(new CardLayout());
        final CardLayout cards = (CardLayout) panel.getLayout();

        final JTextField text = new JTextField();

        final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        final JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));

        final JSpinner dtDateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        final JSpinner dtTimeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));

        final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");

        final java.util.List<Runnable> changeListeners = new ArrayList<Runnable>();

        LiteralValueEditor() {
            // editors
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
            timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm:ss"));
            dtDateSpinner.setEditor(new JSpinner.DateEditor(dtDateSpinner, "yyyy-MM-dd"));
            dtTimeSpinner.setEditor(new JSpinner.DateEditor(dtTimeSpinner, "HH:mm:ss"));

            panel.add(text, "TEXT");
            panel.add(wrapSpinner(dateSpinner), "DATE");
            panel.add(wrapSpinner(timeSpinner), "TIME");
            panel.add(wrapDateTime(dtDateSpinner, dtTimeSpinner), "DATETIME");

            // listeners
            text.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { fireChanged(); }
                public void removeUpdate(DocumentEvent e) { fireChanged(); }
                public void changedUpdate(DocumentEvent e) { fireChanged(); }
            });

            dateSpinner.addChangeListener(e -> fireChanged());
            timeSpinner.addChangeListener(e -> fireChanged());
            dtDateSpinner.addChangeListener(e -> fireChanged());
            dtTimeSpinner.addChangeListener(e -> fireChanged());
        }

        void addChangeListener(Runnable r) { changeListeners.add(r); }

        void fireChanged() {
            for (Runnable r : changeListeners) r.run();
        }

        void setEnabledAll(boolean enabled) {
            text.setEnabled(enabled);
            dateSpinner.setEnabled(enabled);
            timeSpinner.setEnabled(enabled);
            dtDateSpinner.setEnabled(enabled);
            dtTimeSpinner.setEnabled(enabled);
        }

        void setTypeAndValue(ValueType t, String value) {
            if (t == ValueType.DATE) {
                cards.show(panel, "DATE");
                LocalDate ld = tryParseDate(value);
                if (ld != null) dateSpinner.setValue(Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            } else if (t == ValueType.TIME) {
                cards.show(panel, "TIME");
                LocalTime lt = tryParseTime(value);
                if (lt != null) {
                    LocalDate today = LocalDate.now();
                    Date d = Date.from(LocalDateTime.of(today, lt).atZone(ZoneId.systemDefault()).toInstant());
                    timeSpinner.setValue(d);
                }
            } else if (t == ValueType.DATETIME) {
                cards.show(panel, "DATETIME");
                LocalDateTime ldt = tryParseDateTime(value);
                if (ldt != null) {
                    Date d1 = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                    dtDateSpinner.setValue(d1);
                    dtTimeSpinner.setValue(d1);
                }
            } else {
                cards.show(panel, "TEXT");
                text.setText(value == null ? "" : value);
            }
        }

        String getValueAsString(ValueType t) {
            if (t == ValueType.DATE) {
                Date d = (Date) dateSpinner.getValue();
                return dateFmt.format(d);
            } else if (t == ValueType.TIME) {
                Date d = (Date) timeSpinner.getValue();
                return timeFmt.format(d);
            } else if (t == ValueType.DATETIME) {
                Date dDate = (Date) dtDateSpinner.getValue();
                Date dTime = (Date) dtTimeSpinner.getValue();

                LocalDate ld = dDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalTime lt = dTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withNano(0);
                return LocalDateTime.of(ld, lt).toString();
            } else {
                return text.getText();
            }
        }

        private static JPanel wrapSpinner(JSpinner sp) {
            JPanel p = new JPanel(new BorderLayout(6,6));
            p.add(sp, BorderLayout.CENTER);
            return p;
        }

        private static JPanel wrapDateTime(JSpinner date, JSpinner time) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            p.add(new JLabel("Date:"));
            p.add(date);
            p.add(new JLabel("Time:"));
            p.add(time);
            return p;
        }

        private static LocalDate tryParseDate(String s) {
            try { if (s == null || s.trim().isEmpty()) return null; return LocalDate.parse(s.trim()); }
            catch (Exception ex) { return null; }
        }
        private static LocalTime tryParseTime(String s) {
            try {
                if (s == null || s.trim().isEmpty()) return null;
                String v = s.trim();
                if (v.length() == 5) v = v + ":00";
                return LocalTime.parse(v).withNano(0);
            } catch (Exception ex) { return null; }
        }
        private static LocalDateTime tryParseDateTime(String s) {
            try { if (s == null || s.trim().isEmpty()) return null; return LocalDateTime.parse(s.trim()); }
            catch (Exception ex) { return null; }
        }
    }

    // =========================
    // Helpers
    // =========================
    private static JPanel labeled(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(6,6));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(comp, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return row;
    }

    private String firstVarOr(String fallback) {
        List<String> v = reg.variableNames();
        return v.isEmpty() ? fallback : v.get(0);
    }

    // =========================
    // AST child replace (static so dialog can call it)
    // =========================
    private static void replaceChildExpr(Expr parent, int idx, Expr newChild) {
        if (parent instanceof BoolGroup) {
            ((BoolGroup) parent).children.set(idx, newChild);
            return;
        }
        if (parent instanceof Unary) {
            ((Unary) parent).child = newChild;
            return;
        }
        if (parent instanceof BinaryArith) {
            BinaryArith b = (BinaryArith) parent;
            if (idx == 0) b.left = newChild;
            else b.right = newChild;
            return;
        }
        if (parent instanceof FuncCall) {
            ((FuncCall) parent).args.set(idx, newChild);
            return;
        }
        if (parent instanceof Compare) {
            Compare c = (Compare) parent;
            if (idx == 0) c.left = newChild;
            else c.right = newChild;
            return;
        }
        throw new IllegalArgumentException("Unsupported parent for replace: " + parent.getClass());
    }

    // =========================
    // Main
    // =========================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExpressionStudioSwing().setVisible(true));
    }

    // =========================
    // Utility functions
    // =========================
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<parts.size();i++) {
            if (i>0) sb.append(sep);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static double asNum(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        throw new IllegalArgumentException("Expected NUMBER, got: " + typeName(o));
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        throw new IllegalArgumentException("Expected BOOLEAN, got: " + typeName(o));
    }

    private static String typeName(Object o) { return o == null ? "null" : o.getClass().getSimpleName(); }

    private static Object parseByType(ValueType t, String raw, String label) {
        String s = raw == null ? "" : raw.trim();
        try {
            if (t == ValueType.NUMBER) {
                if (s.isEmpty()) throw new IllegalArgumentException(label + ": empty NUMBER");
                return Double.parseDouble(s);
            }
            if (t == ValueType.BOOLEAN) {
                if (s.isEmpty()) throw new IllegalArgumentException(label + ": empty BOOLEAN");
                return Boolean.parseBoolean(s);
            }
            if (t == ValueType.STRING) return s;
            if (t == ValueType.DATE) {
                if (s.isEmpty()) throw new IllegalArgumentException(label + ": empty DATE");
                return LocalDate.parse(s);
            }
            if (t == ValueType.TIME) {
                if (s.isEmpty()) throw new IllegalArgumentException(label + ": empty TIME");
                String v = s.length()==5 ? s + ":00" : s;
                return LocalTime.parse(v).withNano(0);
            }
            if (t == ValueType.DATETIME) {
                if (s.isEmpty()) throw new IllegalArgumentException(label + ": empty DATETIME");
                return LocalDateTime.parse(s);
            }
            throw new IllegalStateException("Unknown type");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + ": invalid NUMBER '" + s + "'");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + ": invalid " + t + " '" + s + "' (use ISO formats)");
        }
    }

    private static Object coerceToType(ValueType t, Object raw, String label) {
        if (t == ValueType.NUMBER) {
            if (raw instanceof Number) return ((Number) raw).doubleValue();
            throw new IllegalArgumentException(label + ": expected NUMBER but got " + typeName(raw));
        }
        if (t == ValueType.BOOLEAN) {
            if (raw instanceof Boolean) return raw;
            throw new IllegalArgumentException(label + ": expected BOOLEAN but got " + typeName(raw));
        }
        if (t == ValueType.STRING) return String.valueOf(raw);
        if (t == ValueType.DATE) {
            if (raw instanceof LocalDate) return raw;
            throw new IllegalArgumentException(label + ": expected DATE but got " + typeName(raw));
        }
        if (t == ValueType.TIME) {
            if (raw instanceof LocalTime) return raw;
            throw new IllegalArgumentException(label + ": expected TIME but got " + typeName(raw));
        }
        if (t == ValueType.DATETIME) {
            if (raw instanceof LocalDateTime) return raw;
            throw new IllegalArgumentException(label + ": expected DATETIME but got " + typeName(raw));
        }
        throw new IllegalStateException("Unknown type");
    }

    private static boolean compareObjects(Object a, Object b, CmpOp op) {
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            if (op == CmpOp.GT) return x > y;
            if (op == CmpOp.GE) return x >= y;
            if (op == CmpOp.LT) return x < y;
            if (op == CmpOp.LE) return x <= y;
            if (op == CmpOp.EQ) return Double.compare(x, y) == 0;
            return Double.compare(x, y) != 0;
        }

        if (a != null && b != null && a.getClass().equals(b.getClass()) && (a instanceof Comparable)) {
            @SuppressWarnings("unchecked")
            int c = ((Comparable) a).compareTo(b);
            if (op == CmpOp.GT) return c > 0;
            if (op == CmpOp.GE) return c >= 0;
            if (op == CmpOp.LT) return c < 0;
            if (op == CmpOp.LE) return c <= 0;
            if (op == CmpOp.EQ) return c == 0;
            return c != 0;
        }

        if (op == CmpOp.EQ) return Objects.equals(a, b);
        if (op == CmpOp.NE) return !Objects.equals(a, b);

        throw new IllegalArgumentException("Incompatible compare: " + typeName(a) + " vs " + typeName(b));
    }

    // Date/time part functions
    private static double yearOf(Object x) {
        if (x instanceof LocalDate) return ((LocalDate) x).getYear();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getYear();
        throw new IllegalArgumentException("year(x) expects DATE or DATETIME, got " + typeName(x));
    }
    private static double monthOf(Object x) {
        if (x instanceof LocalDate) return ((LocalDate) x).getMonthValue();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getMonthValue();
        throw new IllegalArgumentException("month(x) expects DATE or DATETIME, got " + typeName(x));
    }
    private static double dayOf(Object x) {
        if (x instanceof LocalDate) return ((LocalDate) x).getDayOfMonth();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getDayOfMonth();
        throw new IllegalArgumentException("day(x) expects DATE or DATETIME, got " + typeName(x));
    }
    private static double dayOfWeekOf(Object x) {
        if (x instanceof LocalDate) return ((LocalDate) x).getDayOfWeek().getValue();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getDayOfWeek().getValue();
        throw new IllegalArgumentException("dayOfWeek(x) expects DATE or DATETIME, got " + typeName(x));
    }
    private static double hourOf(Object x) {
        if (x instanceof LocalTime) return ((LocalTime) x).getHour();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getHour();
        throw new IllegalArgumentException("hour(x) expects TIME or DATETIME, got " + typeName(x));
    }
    private static double minuteOf(Object x) {
        if (x instanceof LocalTime) return ((LocalTime) x).getMinute();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getMinute();
        throw new IllegalArgumentException("minute(x) expects TIME or DATETIME, got " + typeName(x));
    }
    private static double secondOf(Object x) {
        if (x instanceof LocalTime) return ((LocalTime) x).getSecond();
        if (x instanceof LocalDateTime) return ((LocalDateTime) x).getSecond();
        throw new IllegalArgumentException("second(x) expects TIME or DATETIME, got " + typeName(x));
    }
    private static double epochSecondsOf(Object x) {
        if (x instanceof LocalDateTime) {
            LocalDateTime dt = (LocalDateTime) x;
            return dt.atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        throw new IllegalArgumentException("epochSeconds(x) expects DATETIME, got " + typeName(x));
    }

    // Shortcut helpers
    private static int minutesOfDayOf(Object x) {
        if (x instanceof LocalTime) {
            LocalTime t = (LocalTime) x;
            return t.getHour() * 60 + t.getMinute();
        }
        if (x instanceof LocalDateTime) {
            LocalDateTime dt = (LocalDateTime) x;
            return dt.getHour() * 60 + dt.getMinute();
        }
        throw new IllegalArgumentException("minutesOfDay(x) expects TIME or DATETIME, got " + typeName(x));
    }
    private static LocalTime parseTimeLiteral(Object o) {
        String s = String.valueOf(o).trim();
        if (s.length() == 5) s = s + ":00";
        return LocalTime.parse(s).withNano(0);
    }
    private static LocalDate parseDateLiteral(Object o) {
        String s = String.valueOf(o).trim();
        return LocalDate.parse(s);
    }
}

