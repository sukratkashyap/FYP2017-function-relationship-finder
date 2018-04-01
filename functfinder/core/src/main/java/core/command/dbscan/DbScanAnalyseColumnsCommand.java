package core.command.dbscan;

import com.arangodb.ArangoCursor;
import core.command.CommandException;
import core.command.ICommand;
import core.command.IProgress;
import core.model.CompiledRegression;
import core.model.Data;
import core.model.Regression;
import core.service.IDataService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class DbScanAnalyseColumnsCommand implements ICommand<Collection<CompiledRegression>> {

    private IDataService dataService;
    private Double n1Radius;
    private Integer columnNo;

    public DbScanAnalyseColumnsCommand(IDataService dataService, Double n1Radius, Integer columnNo) {
        this.dataService = dataService;
        this.n1Radius = n1Radius;
        this.columnNo = columnNo;
    }

    @Override
    public Collection<CompiledRegression> execute(IProgress progress) {
        Data any = dataService.findAny();
        if (any == null) {
            throw new CommandException("No data found in the database!");
        }

        if (columnNo == 0) {
            throw new CommandException("Choose parameter column to be analysed cannot analyse output columnNo!");
        }

        List<String> columns = any.getWorkColumns()
                .entrySet()
                .stream()
                .map(m -> format("workColumns.%s", m.getKey()))
                .collect(Collectors.toList());

        columns.parallelStream()
                .forEach(m -> dataService.ensureSkipListIndex(Arrays.asList(m)));

        if (columnNo == -1) {
            List<Integer> colNos = new ArrayList<>();
            for (int i = 1; i < any.getRawColumns().size(); i++) {
                colNos.add(i);
            }
            return analyseAll(progress, n1Radius, colNos, any.getRawColumns().size());
        }

        if (columnNo < 0 || columnNo >= any.getRawColumns().size())

        {
            throw new CommandException("Column number doesnot exist! (Expected: < %d and > 0 and got: )",
                    any.getRawColumns().size(), columnNo);
        }

        progress.setWork(1, "Analysing columns via dbscan!");
        return analyseAll(progress, n1Radius, Arrays.asList(columnNo), any.getRawColumns().size());
    }

    private List<CompiledRegression> analyseAll(IProgress progress, double radius, List<Integer> colNos, int size) {
        Long totalPoints = dataService.count();
        progress.setWork(Math.toIntExact(totalPoints), "Analysing columns via dbscan!");

        List<Data> datas = dataService.findAllIds().asListRemaining();

        List<CompiledRegression> compiledRegressions = datas.parallelStream()
                .flatMap(data -> {
                    Stream<Regression> regressions = colNos.parallelStream()
                            .map(colNo -> analyse(data.getId(), radius, colNo, size))
                            .filter(regression -> regression.getNumOfDataPoints() > 1)
                            .collect(Collectors.toList())
                            .stream();
                    progress.increment();
                    return regressions;
                })
                .collect(Collectors.groupingByConcurrent(regression -> regression.getColNo()))
                .entrySet()
                .parallelStream()
                .map(group ->
                        CompiledRegression.compiledRegression(group.getKey(), group.getValue(),
                                totalPoints, true))
                .collect(Collectors.toList());
        return compiledRegressions;
    }

    private Regression analyse(String id, double radius, int column, int size) {
        int startIndex = 1;
        int endIndex = size;

        String rawQuery = "LET r = FIRST(FOR elem in @@col FILTER elem._id == @id LIMIT 1 RETURN elem)\n"
                + "LET v = (\n"
                + "FOR ng in @@col\n"
                + "%1$s\n"
                + "LET dist = SQRT( %2$s )\n"
                + "FILTER dist <= @radius\n"
                + "COLLECT AGGREGATE\n"
                + format("sX = SUM(ng.rawColumns.%s),\n", Data.colName(column))
                + format("sY = SUM(ng.rawColumns.%s),\n", Data.colName(0)) + //output column
                format("sXX = SUM(POW(ng.rawColumns.%s, 2)),\n", Data.colName(column))
                + format("sXY = SUM(ng.rawColumns.%s * ng.rawColumns.%s),\n", Data.colName(column), Data.colName(0))
                + "n = LENGTH(ng)\n"
                + "return { sX: sX, sY: sY, sXX: sXX, sXY: sXY, n: n })[0]\n"
                + "LET a1 = (v.sX * v.sY) - (v.n * v.sXY)\n"
                + "LET a2 = pow(v.sX, 2) - (v.n * v.sXX)\n"
                + "LET b1 = (v.sX * v.sXY) - (v.sXX * v.sY)\n"
                + "LET b2 = pow(v.sX, 2) - (v.n * v.sXX)\n"
                + "RETURN {\n"
                + "colNo: @colNo,\n"
                + "numOfDataPoints: v.n,\n"
                + "m1: a1,\n"
                + "m2: a2,\n"
                + "c1: b1,\n"
                + "c2: b2\n"
                + "}\n";

        String filter = "";
        String dist = "";
        for (int i = startIndex; i < endIndex; i++) {
            if (i == column) {
                continue;
            }
            filter += format("FILTER (ng.workColumns.%1$s >= -@radius + r.workColumns.%1$s\n", Data.colName(i));
            filter += format("&& ng.workColumns.%1$s <= @radius + r.workColumns.%1$s)\n", Data.colName(i));
            dist += format("POW(ng.workColumns.%1$s - r.workColumns.%1$s, 2) + \n", Data.colName(i));
        }
        dist += "0";

        String query = format(rawQuery, filter, dist);
        ArangoCursor<Regression> regressions = dataService.query(query, new HashMap<String, Object>() {{
            put("radius", radius);
            put("id", id);
            put("colNo", column);
        }}, Regression.class);

        return regressions.asListRemaining().get(0);
    }
}