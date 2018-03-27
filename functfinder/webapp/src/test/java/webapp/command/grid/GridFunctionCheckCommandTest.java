package webapp.command.grid;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import webapp.BaseTest;
import webapp.Faker;
import webapp.command.CommandException;
import webapp.command.CommandProgess;
import webapp.model.Data;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class GridFunctionCheckCommandTest extends BaseTest {

    @Autowired
    private ClusterDataCommand clusterDataCommand;

    @Autowired
    private GridFunctionCheckCommand sut;

    private CommandProgess progress;


    @Override
    public void before() {
        super.before();
        progress = Mockito.mock(CommandProgess.class);
    }

    @Test
    public void shouldWork() throws IOException {
        List<Data> perfectDataFor = Faker.nextData("test.csv", 30, 5);
        operations.insert(perfectDataFor, Data.class);
        clusterDataCommand.execute(progress, Arrays.asList(2.0));

        Resource result = sut.execute(progress, 2.0);

        assertThat(result.contentLength(), is(0l));
    }

    @Test
    public void shouldWorkAndReturnNonEmptyResource_WhereItIsNotAFunction() throws IOException {
        List<Data> perfectDataFor = Faker.nextData("test.csv", 30, 5);
        operations.insert(perfectDataFor, Data.class);
        clusterDataCommand.execute(progress, Arrays.asList(2.0));

        Resource result = sut.execute(progress, 1.0);

        Assert.assertThat(result.contentLength(), greaterThan(0L));
    }

    @Test(expected = CommandException.class)
    public void NoDataShouldThrow() {
        Resource result = sut.execute(progress, 2.0);
    }

    @Test(expected = CommandException.class)
    public void NoGridDataShouldThrow() {
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 5);
        operations.insert(perfectDataFor, Data.class);

        Resource result = sut.execute(progress, 2.0);
    }
}