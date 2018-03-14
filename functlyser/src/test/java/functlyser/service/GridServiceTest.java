package functlyser.service;

import functlyser.BaseSpringTest;
import functlyser.Faker;
import functlyser.exception.ApiException;
import functlyser.model.Data;
import functlyser.model.GridData;
import functlyser.model.Regression;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;


public class GridServiceTest extends BaseSpringTest {

    @Autowired
    private GridService sut;

    @Test
    public void cluster() {
        List<Double> tolerances = Arrays.asList(2.0, 2.0, 2.0, 2.0, 2.0);
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 6);
        arangoOperation.insert(perfectDataFor, Data.class);

        long result = sut.cluster(tolerances);

        assertTrue(arangoOperation.collection(GridData.class).exists());
        assertThat(result, is(5L));
        assertThat(arangoOperation.collection(GridData.class).count().getCount(), is(5L));
    }

    @Test
    public void cluster_withZeroTolerance() {
        List<Double> tolerances = Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0);
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 6);
        arangoOperation.insert(perfectDataFor, Data.class);

        long result = sut.cluster(tolerances);

        assertTrue(arangoOperation.collection(GridData.class).exists());
        assertThat(result, is(10L));
        assertThat(arangoOperation.collection(GridData.class).count().getCount(), is(10L));
    }

    @Test
    public void cluster_withNegativeTolerance() {
        List<Double> tolerances = Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0);
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 6);
        arangoOperation.insert(perfectDataFor, Data.class);

        long result = sut.cluster(tolerances);

        assertTrue(arangoOperation.collection(GridData.class).exists());
        assertThat(result, is(10L));
        assertThat(arangoOperation.collection(GridData.class).count().getCount(), is(10L));
    }

    @Test
    public void cluster_mustTruncateBeforeInsertingAgain() {
        List<Double> tolerances = Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0);
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 6);
        arangoOperation.insert(perfectDataFor, Data.class);

        sut.cluster(tolerances);
        long result = sut.cluster(tolerances);

        assertTrue(arangoOperation.collection(GridData.class).exists());
        assertThat(result, is(10L));
        assertThat(arangoOperation.collection(GridData.class).count().getCount(), is(10L));
    }

    @Test(expected = ApiException.class)
    public void cluster_ThrowIfToleranceAndColumnSizeNotSame() {
        List<Double> tolerances = Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0);
        List<Data> perfectDataFor = Faker.nextData("test.csv", 10, 7);
        arangoOperation.insert(perfectDataFor, Data.class);

        sut.cluster(tolerances);
    }

    @Test(expected = ApiException.class)
    public void cluster_ThrowIfNoData() {
        List<Double> tolerances = Arrays.asList(0.5, 0.5, 0.5, 0.5, 0.5);

        sut.cluster(tolerances);
    }

//    @Test
//    public void testGetFunctionTerminator() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
//        arangoOperation.insert(gridData);
//        gridData = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData);
//
//        List<GridData> result = sut.getFunctionTerminator(2.0);
//
//        assertThat(result.size(), is(0));
//    }
//
//    @Test
//    public void testGetFunctionTerminator_WhereItIsNotAFunction() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
////        gridData.getMembers().get(0).getColumns().set(0, 5.0);
//        arangoOperation.insert(gridData);
//        GridData gridData2 = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData2);
//
//        List<GridData> result = sut.getFunctionTerminator(2.0);
//
//        assertThat(result.size(), is(1));
//        assertThat(result.get(0).getId(), is(gridData.getId()));
//    }
//
//    @Test(expected = ApiException.class)
//    public void testGetFunctionTerminator_NoDataShouldThrow() {
//        List<GridData> result = sut.getFunctionTerminator(2.0);
//    }
//
//    @Test(expected = ApiException.class)
//    public void testGetFunctionTerminator_NoGridDataShouldThrow() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//
//        List<GridData> result = sut.getFunctionTerminator(2.0);
//
//        assertThat(result.size(), is(1));
//    }
//
//    @Test
//    public void testAnalyseColumn() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
//        arangoOperation.insert(gridData);
//        gridData = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData);
//
//        List<Regression> result = sut.analyseColumn(1);
//
//        assertThat(result.size(), is(2));
//        assertTrue(result.stream().allMatch(m->m.getCol() == 1));
//    }
//
//    @Test(expected = ApiException.class)
//    public void testAnalyseColumn_throwWhenIndexMoreThanColumns() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 4);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
//        arangoOperation.insert(gridData);
//        gridData = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData);
//
//        List<Regression> result = sut.analyseColumn(4);
//    }
//
//    @Test(expected = ApiException.class)
//    public void testAnalyseColumn_throwWhenIndexLessThanColumns() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
//        arangoOperation.insert(gridData);
//        gridData = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData);
//
//        List<Regression> result = sut.analyseColumn(-1);
//    }
//
//    @Test(expected = ApiException.class)
//    public void testAnalyseColumn_throwWhenIndexIsOutputColumns() {
//        List<Data> perfectDataFor = getPerfectDataFor(30, "test.csv", 5);
//        arangoOperation.insert(perfectDataFor, Data.class);
//        GridData gridData = getPerfectGroupedData(Arrays.asList(4l, 5l, 6l));
//        arangoOperation.insert(gridData);
//        gridData = getPerfectGroupedData(Arrays.asList(5l, 6l, 7l));
//        arangoOperation.insert(gridData);
//
//        List<Regression> result = sut.analyseColumn(0);    }

}
