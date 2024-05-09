package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public abstract class PreorderSemanticAnalyzer extends PreorderJmmVisitor<Integer,Integer> implements SemanticAnalyzer {

    private final List<Report> reports;

    public PreorderSemanticAnalyzer() {
        this.reports = new ArrayList<>();
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }

    public void addReport(Report report){
        reports.add(report);
    }
}
