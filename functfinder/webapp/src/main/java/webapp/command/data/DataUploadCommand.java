package webapp.command.data;

import com.arangodb.springframework.core.ArangoOperations;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import webapp.command.Command;
import webapp.command.CommandException;
import webapp.command.CommandProgess;
import webapp.model.Data;
import webapp.repository.DataRepository;
import webapp.service.CsvService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
public class DataUploadCommand implements Command<DataUploadCommand.Param, Long> {

    private DataRepository dataRepository;

    private ArangoOperations operations;

    private CsvService csvService;

    @Autowired
    public DataUploadCommand(DataRepository dataRepository, ArangoOperations operations, CsvService csvService) {
        this.dataRepository = dataRepository;
        this.operations = operations;
        this.csvService = csvService;
    }

    @Override
    public synchronized Long execute(CommandProgess progress, Param param) {
        String fileName = param.getFileName();
        progress.setTotalWork(2, "'%s' file upload started!", fileName);

        Data anyData = dataRepository.findFirstByFileName(fileName);
        if (anyData != null) {
            throw new CommandException("'%s' already exists!", fileName);
        }
        anyData = dataRepository.findFirstByRawColumnsNotNull();

        List<Data> datas = null;
        Function<Map<String, Object>, Data> converter = (row) -> {
            Data data = new Data();
            data.setFileName(fileName);
            Map<String, Double> collect = row.entrySet()
                    .stream()
                    .collect(Collectors
                            .toMap(m -> m.getKey(), m -> (Double) m.getValue()));
            data.setRawColumns(collect);
            data.setWorkColumns(collect);
            return data;
        };
        if (anyData != null) {
            int colSize = anyData.getRawColumns().size();
            datas = csvService.convert(param.getInputStream(), true,
                    header -> {
                        if (header.length != colSize) {
                            throw new CommandException("Number of columns donot match! (expected: %d, actual: %d)",
                                    colSize, header.length);
                        }
                        return getArgumentsForCsv(header.length);
                    }, converter);
        } else {
            datas = csvService.convert(param.getInputStream(), true,
                    header -> {
                        if (header.length == 1) {
                            throw new CommandException("Number of columns must be greater than 1!");
                        }
                        return getArgumentsForCsv(header.length);
                    }, converter);
        }
        progress.update(1, "'%s' parsed successfully!", fileName);
        progress.setTotalWork(datas.size(), "Entering data into database!");
        AtomicInteger done = new AtomicInteger(0);
        datas.parallelStream()
                .forEach(m -> {
                    operations.insert(m);
                    progress.update(done.incrementAndGet());
                });
        progress.update(2, "'%s' saved successfully!", fileName);
        return datas.stream().count();
    }

    private Pair<String[], CellProcessor[]> getArgumentsForCsv(int size) {
        CellProcessor[] processors = new CellProcessor[size];
        String[] headers = new String[size];
        for (int i = 0; i < size; i++) {
            headers[i] = format("%s%d", Data.prefixColumn, i);
            processors[i] = new NotNull(new ParseDouble());
        }
        return new Pair<>(headers, processors);
    }

    public static class Param {
        private InputStream inputStream;
        private String fileName;

        public Param(InputStream inputStream, String fileName) {
            this.inputStream = inputStream;
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public InputStream getInputStream() {
            return inputStream;
        }
    }
}